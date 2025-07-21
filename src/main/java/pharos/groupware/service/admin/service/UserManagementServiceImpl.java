package pharos.groupware.service.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.infrastructure.graph.GraphUserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
import pharos.groupware.service.team.service.UserService;

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
        return keycloakId;
    }

    @Override
    public String deleteUser(String userId) {
        return "";
    }
}
