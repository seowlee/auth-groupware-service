package pharos.groupware.service.auth.service;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.infrastructure.keycloak.KeycloakAdminClientFactory;

import java.util.Collections;

@Slf4j
@Service
public class KeycloakAuthServiceImpl implements KeycloakAuthService {

    private final KeycloakAdminClientFactory factory;
    private final LocalAuthService localAuthService;

    public KeycloakAuthServiceImpl(KeycloakAdminClientFactory factory, LocalAuthService localAuthService) {
        this.factory = factory;
        this.localAuthService = localAuthService;
    }

    @Override
    @Transactional
    public String createUser(CreateUserReqDTO reqDTO) {
        // Define user
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(reqDTO.getUsername());
        user.setEmail(reqDTO.getEmail());
        user.setEmailVerified(false);
        user.setFirstName(reqDTO.getFirstName());
        user.setLastName(reqDTO.getLastName());

        // Define password credential
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(reqDTO.getRawPassword());

        user.setCredentials(Collections.singletonList(credential));

        // Get realm
        UsersResource usersResource = factory.realmResource().users();

        // Create user
        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Keycloak user create failed: " + response.getStatus());
        }

        // get new userid
        String newUserUUID = CreatedResponseUtil.getCreatedId(response);
        reqDTO.setUserUUID(newUserUUID);
        response.close();

        // Assign group to user
        GroupRepresentation group = factory.realmResource()
                .groups()
                .groups()
                .stream()
                .filter(g -> g.getName().equals(reqDTO.getRole().getGroupName()))
                .findFirst()
                .orElseThrow();

        factory.realmResource()
                .users()
                .get(newUserUUID)
                .joinGroup(group.getId());

        localAuthService.createUser(reqDTO);

        return newUserUUID;
    }

    ;

    @Override
    @Transactional
    public void deleteUser(String userId) {
        factory.realmResource().users().get(userId).remove();
    }


}
