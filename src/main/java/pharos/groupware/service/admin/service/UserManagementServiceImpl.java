package pharos.groupware.service.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
import pharos.groupware.service.team.service.UserService;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final KeycloakUserService keycloakUserService;
    private final GraphUserService graphUserService;
    private final UserService localUserService;

    @Override
    @Transactional
    public String createUser(CreateUserReqDto reqDto) {
        String keycloakId = keycloakUserService.createUser(reqDto);
        reqDto.setUserUUID(keycloakId);
        String graphUserId = graphUserService.createUser(reqDto);
        System.out.println("graphUserId: " + graphUserId);
        graphUserService.assignLicenseToUser(graphUserId);
        localUserService.createUser(reqDto);
        log.info("사용자 생성 완료 | keycloakId: {}, graphUserId: {}", keycloakId, graphUserId);
//        auditLogService.record("USER_CREATE", "superadmin", keycloakId);
        return keycloakId;
    }

    @Override
    public void deleteUser(String keycloakUserId, String graphUserId) {
        graphUserService.deleteUser(graphUserId);
        keycloakUserService.deleteUser(keycloakUserId);
        log.info("Deleted user. keycloakId={}, graphUserId={}", keycloakUserId, graphUserId);
    }

    @Override
    public void deactivateUser(String userId) {

    }
}
