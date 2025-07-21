package pharos.groupware.service.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KeycloakTokenProvider {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public KeycloakTokenProvider(
            RestClient.Builder builder,
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = builder.baseUrl(tokenUri).build();
    }

    public String getAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        Map<String, Object> response = restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Keycloak did not return access_token");
        }

        return (String) response.get("access_token");
    }
}

