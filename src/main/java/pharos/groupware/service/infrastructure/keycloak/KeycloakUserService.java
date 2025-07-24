package pharos.groupware.service.infrastructure.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.admin.dto.CreateUserReqDto;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class KeycloakUserService {

    private final KeycloakTokenProvider tokenProvider;
    private final RestClient.Builder restClientBuilder;
    private final String baseUrl;

    public KeycloakUserService(
            KeycloakTokenProvider tokenProvider,
            RestClient.Builder builder,
            @Value("${keycloak.auth-server-url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm
    ) {
        this.tokenProvider = tokenProvider;
        this.restClientBuilder = builder;
        this.baseUrl = keycloakUrl + "/admin/realms/" + realm;
    }

    private RestClient withAuth() {
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .build();
    }

    public String createUser(CreateUserReqDto reqDto) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", reqDto.getUsername());
        user.put("enabled", true);
        user.put("email", reqDto.getEmail());
        user.put("firstName", reqDto.getFirstName());
        user.put("lastName", reqDto.getLastName());
        user.put("credentials", List.of(Map.of(
                "type", "password",
                "value", reqDto.getRawPassword(),
                "temporary", false
        )));

        ResponseEntity<Void> response = withAuth().post()
                .uri("/users")
                .body(user)
                .retrieve()
                .toBodilessEntity();

        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new RuntimeException("사용자 생성 실패 (location 헤더 없음)");
        }

        return location.getPath().replaceAll(".*/", "");
    }

    ;

    public void deleteUser(String userId) {
        withAuth().delete()
                .uri("/users/{id}", userId)
                .retrieve()
                .toBodilessEntity();
    }


    public void deactivateUser(String userId) {
        Map<String, Object> body = Map.of("enabled", false);
        withAuth().put()
                .uri("/users/{id}", userId)
                .contentType(APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
