package pharos.groupware.service.infrastructure.graph;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;


@Service
public class GraphApiUserClient {

    private final WebClient graphWebClient;

    public GraphApiUserClient(WebClient graphWebClient) {
        this.graphWebClient = graphWebClient;
    }

    public Mono<Map> listUsers() {
        return graphWebClient.get()
                .uri("https://graph.microsoft.com/v1.0/users")
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 특정 사용자 조회
    public Mono<Map> getUserById(@PathVariable String id) {
        return graphWebClient.get()
                .uri("https://graph.microsoft.com/v1.0/users/{id}", id)
                .retrieve()
                .bodyToMono(Map.class);
    }

    public Mono<Map> createUser(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String rawPassword = req.get("rawPassword");
        String email = req.get("email");
        String firstName = req.get("firstName");
        String lastName = req.get("lastName");

        Map<String, Object> body = Map.of(
                "accountEnabled", true,
                "displayName", firstName + " " + lastName,
                "mailNickname", username,
                "userPrincipalName", email,
                "passwordProfile", Map.of(
                        "forceChangePasswordNextSignIn", false,
                        "password", rawPassword
                )
        );

        return graphWebClient.post()
                .uri("https://graph.microsoft.com/v1.0/users")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class);
    }

}

