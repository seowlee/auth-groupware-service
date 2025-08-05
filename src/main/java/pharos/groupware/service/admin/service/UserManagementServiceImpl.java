package pharos.groupware.service.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.admin.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
import pharos.groupware.service.leave.service.LeaveBalanceService;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;
import pharos.groupware.service.team.service.UserService;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final KeycloakUserService keycloakUserService;
    private final GraphUserService graphUserService;
    private final UserService localUserService;
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
            int yearNumber = DateUtils.getYearsOfService(reqDto.getJoinedDate());
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
    public String deactivateUser(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);

        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        try {
            keycloakUserService.deactivateUser(keycloakUserId);
            localUserService.deactivateUser(user);
            log.info("Deactivated user. userUUID={}", keycloakUserId);
            return keycloakUserId;
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

    @Override
    @Transactional
    public String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        String currentUsername = AuthUtils.getCurrentUsername();
        String keycloakUserId = user.getUserUuid().toString();
        user.updateByAdmin(reqDto, currentUsername);

        // Keycloak에도 반영할 필드가 있을 경우
        try {
            if (needToSyncWithKeycloak(reqDto)) {
                keycloakUserService.updateUserProfile(keycloakUserId, reqDto);
            }
        } catch (Exception e) {
            log.error("Keycloak 업데이트 실패", e);
            throw new RuntimeException("사용자 정보는 저장되었으나 Keycloak 동기화 실패", e);
        }
        return keycloakUserId;
    }


    /**
     * 로컬 DB의 PENDING 사용자 승인 처리
     * 1) 임시 비밀번호 생성
     * 2) Keycloak에 User 생성
     * 3) Kakao IdP 연동
     * 4) 로컬 User 엔티티 approve(...) 호출 및 저장
     */
    @Transactional
    public void approvePendingUser(UUID localUserUuid) {
        // 1. 로컬 사용자 조회
        User localUser = userRepository.findByUserUuid(localUserUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + localUserUuid));

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
                localUser.getUsername(),      // 로컬에 저장된 카카오 sub
                localUser.getEmail()       // 로컬에 저장된 카카오 username
        );

        // 5. 로컬 User 엔티티에 승인 처리
        localUser.approve(encodedPwd, keycloakUserId);
        // JPA가 트랜잭션 커밋 시점에 자동으로 변경 감지 후 저장해 줌

        // 6. (선택) 임시 비밀번호 로그에 기록 또는 알림 전송
        log.info("User {} approved with temp password {}", localUser.getEmail(), tempPlain);
    }


    private boolean needToSyncWithKeycloak(UpdateUserByAdminReqDto reqDto) {
        return reqDto.getEmail() != null
                || reqDto.getFirstName() != null
                || reqDto.getLastName() != null;
    }

}
