package pharos.groupware.service.common.enums;

public enum KeycloakRoleEnum {
    ADMIN("ADMIN"),
    USER("USER");

    private final String name;

    KeycloakRoleEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
