package pharos.groupware.service.infrastructure.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.team.service.UserService;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KeycloakUserService {

    private final RestClient restClient;
    private final KeycloakTokenProvider tokenProvider;

    public KeycloakUserService(
            KeycloakTokenProvider tokenProvider,
            UserService localAuthService,
            RestClient.Builder builder,
            @Value("${keycloak.auth-server-url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm
    ) {
        this.tokenProvider = tokenProvider;
        this.restClient = builder
                .baseUrl(keycloakUrl + "/admin/realms/" + realm)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        ;
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

        ResponseEntity<Void> response = restClient.post()
                .uri("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
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
        restClient.delete()
                .uri("/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .retrieve()
                .toBodilessEntity();
    }


}
