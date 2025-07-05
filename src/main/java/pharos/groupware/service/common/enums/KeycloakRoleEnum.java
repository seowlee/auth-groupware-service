package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum KeycloakRoleEnum {
    ADMIN("ADMIN", "admin-group"),
    USER("USER", "user-group");

    private final String roleName;
    private final String groupName;


    KeycloakRoleEnum(String roleName, String groupName) {
        this.roleName = roleName;
        this.groupName = groupName;
    }
}
