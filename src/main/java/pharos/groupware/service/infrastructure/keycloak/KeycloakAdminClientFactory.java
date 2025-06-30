package pharos.groupware.service.infrastructure.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAdminClientFactory {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakAdminClientFactory(Keycloak keycloak,
                                      @Value("${keycloak.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    public RealmResource realmResource() {
        return keycloak.realm(realm);
    }
}
