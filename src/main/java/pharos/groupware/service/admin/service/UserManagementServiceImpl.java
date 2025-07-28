package pharos.groupware.service.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
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
    private final UserRepository userRepository;

    @Override
    @Transactional
    public String createUser(CreateUserReqDto reqDto) {
        String keycloakId = null;
        try {
            keycloakId = keycloakUserService.createUser(reqDto);
            reqDto.setUserUUID(keycloakId);
//        String graphUserId = graphUserService.createUser(reqDto);
//        graphUserService.assignLicenseToUser(graphUserId);
            localUserService.createUser(reqDto);
            log.info("사용자 생성 완료 | userUUID: {}", keycloakId);
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
}
