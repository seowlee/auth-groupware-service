package pharos.groupware.service.common.enums;

public enum AuthProviderEnum {
    LOCAL("LOCAL"), OAUTH2("OAUTH2"), OIDC("OIDC");
    //oauth2생략
    private final String name;

    AuthProviderEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

