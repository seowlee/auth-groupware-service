package pharos.groupware.service.auth.dto;

import lombok.Data;
import pharos.groupware.service.common.enums.KeycloakRoleEnum;

@Data
public class CreateUserReqDTO {
    private String userUUID;
    private String username;
    private String rawPassword;
    private String email;
    private String firstName;
    private String lastName;
    private KeycloakRoleEnum role;
}
