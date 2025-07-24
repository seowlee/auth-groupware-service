package pharos.groupware.service.admin.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
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
    public void deleteUser(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);
        boolean exists = userRepository.existsByUserUuid(uuid);
        if (!exists) throw new EntityNotFoundException("사용자 없음");

//        graphUserService.deleteUser(graphUserId);

        keycloakUserService.deleteUser(keycloakUserId);
        localUserService.deleteUser(uuid);
        log.info("Deleted user. keycloakId={}", keycloakUserId);
//        auditLogService.record("USER_DELETE", "SUCCESS", "Deleted user_uuid: " + keycloakUserId);
    }

    @Override
    public String deactivateUser(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);
        boolean exists = userRepository.existsByUserUuid(uuid);
        if (!exists) throw new EntityNotFoundException("사용자 없음");

        keycloakUserService.deactivateUser(keycloakUserId);
        localUserService.deactivateUser(uuid);
        return keycloakUserId;
    }
}
