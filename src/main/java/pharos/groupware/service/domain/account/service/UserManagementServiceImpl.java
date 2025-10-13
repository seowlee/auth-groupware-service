package pharos.groupware.service.domain.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.common.util.PartitionUtils;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.audit.service.AuditLogService;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static pharos.groupware.service.common.util.AuditLogUtils.details;
import static pharos.groupware.service.common.util.DateUtils.KST;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final KeycloakUserService keycloakUserService;
    private final GraphUserService graphUserService;
    private final UserService localUserService;
    private final UserDeletionTxService userDeletionTxService;
    private final LeaveBalanceService leaveBalanceService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public String createUser(CreateUserReqDto reqDto, AppUser actor) {
        String keycloakId = null;
        try {
            keycloakId = keycloakUserService.createUser(reqDto);
            reqDto.setUserUUID(keycloakId);
//        String graphUserId = graphUserService.createUser(reqDto);
//        graphUserService.assignLicenseToUser(graphUserId);
            Integer yearNumber = LeaveUtils.getCurrentYearNumber(reqDto.getJoinedDate());
            reqDto.setYearNumber(yearNumber);
            Long userId = localUserService.createUser(reqDto, actor.username());
            log.info("사용자 생성 완료 | userUUID: {}", keycloakId);
            leaveBalanceService.initializeLeaveBalancesForUser(userId, yearNumber);
            auditLogService.saveLog(
                    actor.id(),
                    actor.username(),
                    AuditActionEnum.USER_CREATE,
                    AuditStatusEnum.SUCCESS,
                    details("reqDto", reqDto, "localUserId", userId)
            );
            return keycloakId;
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            if (keycloakId != null) keycloakUserService.deleteUser(keycloakId);
            throw dup;

        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            auditLogService.saveLog(
                    actor != null ? actor.id() : null,
                    actor != null ? actor.username() : "system",
                    AuditActionEnum.USER_CREATE,
                    AuditStatusEnum.FAILED,
                    details("reqDto", reqDto, "error", e.getMessage())
            );
            if (keycloakId != null) {
                keycloakUserService.deleteUser(keycloakId);
            }
            throw new IllegalStateException("사용자 생성에 실패했습니다");
        }
    }

    @Override
    @Transactional
    public void deleteUser(String keycloakUserId, AppUser actor) {
        UUID uuid = UUID.fromString(keycloakUserId);
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        Long targetUserId = user.getId();
        String username = user.getUsername();
        try {
            //        graphUserService.deleteUser(graphUserId);
            keycloakUserService.deleteUser(keycloakUserId);
            localUserService.deleteUser(user);
            log.info("Deleted user. userUUID={}", keycloakUserId);

            auditLogService.saveLog(
                    actor.id(),
                    actor.username(),
                    AuditActionEnum.USER_DELETE, AuditStatusEnum.SUCCESS,
                    details("targetUserId", targetUserId, "keycloakUserId", keycloakUserId, "username", username)
            );

        } catch (Exception e) {
            log.error("failed to delete user. keycloakUserId={}", keycloakUserId, e);
            auditLogService.saveLog(
                    actor != null ? actor.id() : null,
                    actor != null ? actor.username() : "system",
                    AuditActionEnum.USER_DELETE, AuditStatusEnum.FAILED,
                    details("targetUserId", targetUserId, "keycloakUserId", keycloakUserId, "username", username, "error", e.getMessage())
            );
            throw new IllegalStateException("사용자 삭제에 실패했습니다");
        }

    }

    @Override
    @Transactional
    public String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto, AppUser actor) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        //  변경 전 스냅샷 (joinedDate/yearNumber 비교용)
        LocalDate beforeJoined = user.getJoinedDate();
        Integer beforeYearNo = user.getYearNumber();

        // 상태 전이 판단용
        UserStatusEnum beforeStatus = user.getStatus();
        UserStatusEnum afterStatus = reqDto.getStatus() != null
                ? UserStatusEnum.valueOf(reqDto.getStatus())
                : beforeStatus;

        boolean kcSynced = false;
        String kcSyncError = null;

        try {
            // 1) 기본 정보 수정
            localUserService.update(user, reqDto);

            // 2) 입사일 변경 → 연차 한도 재배정
            LocalDate afterJoined = user.getJoinedDate();
            Integer afterYearNo = user.getYearNumber();
            boolean joinedChanged = !java.util.Objects.equals(beforeJoined, afterJoined);
            if (joinedChanged) {
                leaveBalanceService.reallocateAnnualOnJoinedDateChange(user.getId(), beforeYearNo, afterYearNo);
            }
            // 3) 상태 전이 오케스트레이션
            if (beforeStatus != afterStatus) {
                if (beforeStatus == UserStatusEnum.PENDING && afterStatus == UserStatusEnum.ACTIVE) {
                    approvePendingUser(user);
                    return user.getUserUuid().toString();
                } else if (beforeStatus == UserStatusEnum.ACTIVE && afterStatus == UserStatusEnum.INACTIVE) {
                    deactivateUser(user);
                } else if (beforeStatus == UserStatusEnum.INACTIVE && afterStatus == UserStatusEnum.ACTIVE) {
                    reactivateUser(user); // 새로 추가: Keycloak enable + user.activate()
                } else {
                    throw new ResponseStatusException(BAD_REQUEST,
                            "허용되지 않는 상태 전이: " + beforeStatus + " → " + afterStatus);
                }
            }

            // 4) Keycloak 동기화(필요 시)
            if (needToSyncWithKeycloak(reqDto)) {
                try {
                    keycloakUserService.updateUserProfile(user.getUserUuid().toString(), reqDto);
                    kcSynced = true;
                } catch (Exception ke) {
                    kcSynced = false;
                    kcSyncError = ke.getMessage();
                    throw new RuntimeException("Keycloak 동기화 실패", ke);
                }
            }
            // 5)  감사 로그
            auditLogService.saveLog(
                    actor.id(),
                    actor.username(),
                    AuditActionEnum.USER_UPDATE,
                    AuditStatusEnum.SUCCESS,
                    details(
                            "targetUserId", user.getId(),
                            "reqDto", reqDto,
                            "beforeJoinedDate", beforeJoined,
                            "afterJoinedDate", afterJoined,
                            "beforeYearNumber", beforeYearNo,
                            "afterYearNumber", afterYearNo,
                            "joinedDateChanged", joinedChanged,
                            "beforeStatus", beforeStatus.name(),
                            "requestedStatus", afterStatus.name(),
                            "keycloakSynced", kcSynced
                    )
            );
        } catch (Exception e) {
            auditLogService.saveLog(
                    actor != null ? actor.id() : null,
                    actor != null ? actor.username() : "system",
                    AuditActionEnum.USER_UPDATE, AuditStatusEnum.FAILED,
                    details(
                            "targetUserId", user.getId(),
                            "reqDto", reqDto,
                            "error", e.getMessage(),
                            "keycloakSynced", kcSynced,
                            "keycloakSyncError", kcSyncError
                    )
            );
            throw e;
        }
        return user.getUserUuid().toString();
    }

    public void deactivateUser(User user) {

        String keycloakUserId = user.getUserUuid().toString();
        try {
            keycloakUserService.deactivateUser(keycloakUserId);
            localUserService.deactivateUser(user);
            log.info("Deactivated user. userUUID={}", keycloakUserId);
        } catch (Exception e) {
            log.error("사용자 비활성화 중 DB 오류 발생. Keycloak 사용자 복원 시도", e);
            // 복구 시도
            try {
                keycloakUserService.reactivateUser(keycloakUserId); // 보상 트랜잭션
            } catch (Exception rollbackEx) {
                log.error("Keycloak 사용자 복구 실패! ", rollbackEx);
            }
            throw e;
        }
    }

    public void reactivateUser(User user) {
        String keycloakUserId = user.getUserUuid().toString();
        try {
            keycloakUserService.reactivateUser(keycloakUserId);
            localUserService.activate(user);
        } catch (Exception e) {
            log.error("사용자 활성화 중 DB 오류 발생. ", e);
        }
    }

    /**
     * 로컬 DB의 PENDING 사용자 승인 처리
     * 1) 임시 비밀번호 생성
     * 2) Keycloak에 User 생성
     * 3) Kakao IdP 연동
     * 4) 로컬 User 엔티티 approve(...) 호출 및 저장
     */
    public void approvePendingUser(User localUser) {

        // 2. 임시 비밀번호 생성 (8자리)
        String tempPlain = "1234";
        String encodedPwd = passwordEncoder.encode(tempPlain);

        // 3. Keycloak 사용자 생성 (평문 임시 비밀번호로)
        CreateUserReqDto reqDto = new CreateUserReqDto();
        reqDto.setUsername(localUser.getUsername());
        reqDto.setEmail(localUser.getEmail());
        reqDto.setFirstName(localUser.getFirstName());
        reqDto.setLastName(localUser.getLastName());
        reqDto.setRawPassword(tempPlain);
        String keycloakUserId = keycloakUserService.createUser(reqDto);

        // 4. Kakao IdP 연동 (sub, username 등 로컬에 저장된 값으로)
        keycloakUserService.linkKakaoFederatedIdentity(
                keycloakUserId,
                localUser.getKakaoSub(),      // 로컬에 저장된 카카오 sub
                localUser.getEmail()       // 로컬에 저장된 카카오 username
        );

        // 5. 로컬 User 엔티티에 승인 처리
        localUser.approve(encodedPwd, keycloakUserId);
        // JPA가 트랜잭션 커밋 시점에 자동으로 변경 감지 후 저장해 줌
        leaveBalanceService.initializeLeaveBalancesForUser(localUser.getId(), localUser.getYearNumber());


        // 6. (선택) 임시 비밀번호 로그에 기록 또는 알림 전송
        log.info("User {} approved with temp password {}", localUser.getEmail(), tempPlain);
    }


    @Override
    @Transactional
    public void deleteUsersOlderThanDays(int days) {
        OffsetDateTime cutoff = OffsetDateTime.now(KST).minusDays(days);
        List<UUID> targets = userRepository.findInactiveUserIdsOlderThan(cutoff);
        List<UUID> ok = new ArrayList<>();
        int fail = 0;
        for (UUID id : targets) {
            try {
                userDeletionTxService.deleteKeycloakOnly(id.toString());
                ok.add(id);
            } catch (Exception e) {
                fail++;
                log.error("keycloak delete failed: {}", id, e);
                auditLogService.saveJobLog(
                        "delete-inactive-users",
                        AuditActionEnum.USER_DELETE_KEYCLOAK,
                        AuditStatusEnum.FAILED,
                        details(
                                "targetUserUuid", id,
                                "error", e.getMessage(),
                                "cutoff", cutoff,
                                "days", days
                        )
                );
            }
        }
        for (List<UUID> chunk : PartitionUtils.batchesOf(ok, 1000)) {
            userRepository.deleteByUserUuidIn(chunk);
        }
        auditLogService.saveJobLog(
                "delete-inactive-users",
                AuditActionEnum.USER_DELETE_KEYCLOAK,
                AuditStatusEnum.SUCCESS,
                details(
                        "days", days,
                        "okCount", ok.size(),
                        "failCount", fail,
                        "cutoff", cutoff
                )
        );
        log.info("Purge INACTIVE users finished. days={}, ok={}, fail={}, cutoff={}", days, ok.size(), fail, cutoff);
    }

    private boolean needToSyncWithKeycloak(UpdateUserByAdminReqDto reqDto) {
        return reqDto.getFirstName() != null
                || reqDto.getLastName() != null;
    }
}
