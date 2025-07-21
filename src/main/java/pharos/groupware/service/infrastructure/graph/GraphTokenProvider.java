package pharos.groupware.service.infrastructure.graph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GraphTokenProvider {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    public GraphTokenProvider(
            RestClient.Builder builder,
            @Value("${graph.token-uri}") String tokenUri,
            @Value("${graph.client-id}") String clientId,
            @Value("${graph.client-secret}") String clientSecret,
            @Value("${graph.scope}") String scope
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.restClient = builder.baseUrl(tokenUri).build();
    }

    public String getAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("scope", scope);

        Map<String, Object> response = restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return (String) response.get("access_token");
    }
}

