package pharos.groupware.service.domain.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.common.util.PartitionUtils;
import pharos.groupware.service.common.util.PhoneNumberUtils;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.PendingUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.account.dto.UserApplicantResDto;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public String createUser(CreateUserReqDto reqDto) {
        String keycloakId = null;
        String currentUsername = AuthUtils.getCurrentUsername();
        try {
            keycloakId = keycloakUserService.createUser(reqDto);
            reqDto.setUserUUID(keycloakId);
//        String graphUserId = graphUserService.createUser(reqDto);
//        graphUserService.assignLicenseToUser(graphUserId);
            Integer yearNumber = LeaveUtils.getCurrentYearNumber(reqDto.getJoinedDate());
            reqDto.setYearNumber(yearNumber);
            Long userId = localUserService.createUser(reqDto, currentUsername);
            log.info("사용자 생성 완료 | userUUID: {}", keycloakId);

            leaveBalanceService.initializeLeaveBalancesForUser(userId, yearNumber);
//        auditLogService.record("USER_CREATE", "superadmin", keycloakId);
            return keycloakId;
        } catch (Exception e) {
            log.error("사용자 생성 실패", e);
            if (keycloakId != null) {
                keycloakUserService.deleteUser(keycloakId);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteUser(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        try {
            //        graphUserService.deleteUser(graphUserId);
            keycloakUserService.deleteUser(keycloakUserId);
            localUserService.deleteUser(user);
            log.info("Deleted user. userUUID={}", keycloakUserId);
//        auditLogService.record("USER_DELETE", "SUCCESS", "Deleted user_uuid: " + keycloakUserId);

        } catch (Exception e) {
            log.error("failed to delete user. keycloakUserId={}", keycloakUserId, e);
        }

    }

    @Override
    @Transactional
    public String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        UserStatusEnum before = user.getStatus();
        UserStatusEnum after = reqDto.getStatus() != null
                ? UserStatusEnum.valueOf(reqDto.getStatus())
                : before;

        // 1) 기본 정보 수정
        localUserService.update(user, reqDto);


        // 2) 상태 전이 오케스트레이션
        if (before != after) {
            if (before == UserStatusEnum.PENDING && after == UserStatusEnum.ACTIVE) {
                approvePendingUser(user);
                return user.getUserUuid().toString();
            } else if (before == UserStatusEnum.ACTIVE && after == UserStatusEnum.INACTIVE) {
                deactivateUser(user);
            } else if (before == UserStatusEnum.INACTIVE && after == UserStatusEnum.ACTIVE) {
                reactivateUser(user); // 새로 추가: Keycloak enable + user.activate()
            } else {
                throw new ResponseStatusException(BAD_REQUEST,
                        "허용되지 않는 상태 전이: " + before + " → " + after);
            }
        }

        // Keycloak에도 반영할 필드가 있을 경우
        try {
            if (needToSyncWithKeycloak(reqDto)) {
                String keycloakUserId = user.getUserUuid().toString();
                keycloakUserService.updateUserProfile(keycloakUserId, reqDto);
            }
        } catch (Exception e) {
            log.error("Keycloak 업데이트 실패", e);
            throw new RuntimeException("사용자 정보는 저장되었으나 Keycloak 동기화 실패", e);
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
    public void registerOrLinkSocialUser(PendingUserReqDto reqDto) {
        String normalizedPhone = PhoneNumberUtils.normalize(reqDto.getPhoneNumber());
        String kakaoSub = reqDto.getProviderUserId();

//        if (normalizedPhone == null || normalizedPhone.isBlank()) {
//            throw new IllegalArgumentException("휴대전화 번호가 필요합니다.");
//        }
        if (kakaoSub == null || kakaoSub.isBlank()) {
            throw new IllegalArgumentException("Kakao sub가 필요합니다.");
        }

        // 0) sub가 이미 다른 사용자에 등록되어 있으면 막기
//        Optional<User> existingBySub = userRepository.findByKakaoSub(kakaoSub);
//        if (existingBySub.isPresent()) {
//            return; // 이미 등록되어 있으면 조용히 성공 처리하거나, 예외로 알림(정책)
//        }

        // 1) 휴대폰으로 기존 사용자 찾기
        Optional<User> existingUserOpt = userRepository.findByPhoneNumber(normalizedPhone);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // 1-a) 해당 사용자가 Keycloak 계정을 이미 가지고 있으면 → 그 계정에 Kakao 연동
            String keycloakUserId = String.valueOf(existingUser.getUserUuid());
            String kakaoUsername = reqDto.getEmail();
            keycloakUserService.linkKakaoFederatedIdentity(
                    keycloakUserId,
                    kakaoSub,
                    kakaoUsername
            );
            // 로컬에도 반영
            localUserService.linkKakaoLocally(existingUser, kakaoSub);
            return;

        }
        localUserService.registerPendingUser(reqDto);
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
            }
        }
        for (List<UUID> chunk : PartitionUtils.batchesOf(ok, 1000)) {
            userRepository.deleteByUserUuidIn(chunk);
        }
        log.info("Purge INACTIVE users finished. days={}, ok={}, fail={}, cutoff={}", days, ok.size(), fail, cutoff);
    }

    @Override
    public List<UserApplicantResDto> findAllApplicants(String q) {
        return userRepository.findActiveUsersForSelect(q)
                .stream()
                .map(UserApplicantResDto::toApplicantDto)
                .toList();
    }

    private boolean needToSyncWithKeycloak(UpdateUserByAdminReqDto reqDto) {
        return reqDto.getEmail() != null
                || reqDto.getFirstName() != null
                || reqDto.getLastName() != null;
    }
}
