package pharos.groupware.service.infrastructure.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.domain.admin.dto.CreateUserReqDto;
import pharos.groupware.service.domain.admin.dto.UpdateUserByAdminReqDto;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        user.put("requiredActions", List.of("UPDATE_PASSWORD"));

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

    public void reactivateUser(String userId) {
        Map<String, Object> body = Map.of("enabled", true);
        withAuth().put()
                .uri("/users/{id}", userId)
                .contentType(APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }


    public void updateUserProfile(String keycloakUserId, UpdateUserByAdminReqDto reqDto) {
        Map<String, Object> updates = new HashMap<>();

        if (reqDto.getFirstName() != null) updates.put("firstName", reqDto.getFirstName());
        if (reqDto.getLastName() != null) updates.put("lastName", reqDto.getLastName());
        if (reqDto.getEmail() != null) updates.put("email", reqDto.getEmail());
//        if (reqDto.getUsername() != null)  updates.put("username", reqDto.getUsername());

        withAuth().put()
                .uri("/users/{id}", keycloakUserId)
                .body(updates)
                .retrieve()
                .toBodilessEntity();
    }

    public void linkKakaoFederatedIdentity(String keycloakUserId, String kakaoUserId, String kakaoUsername) {
        Map<String, Object> body = Map.of(
                "userId", kakaoUserId,
                "userName", kakaoUsername
        );

        withAuth().post()
                .uri("/users/{id}/federated-identity/kakao", keycloakUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public boolean isLinked(String keycloakUserId, String idpAlias) {
        List<Map<String, Object>> list = withAuth().get()
                .uri("/users/{id}/federated-identity", keycloakUserId)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<Map<String, Object>>>() {
                });

        return list != null && list.stream()
                .anyMatch(m -> idpAlias.equals(m.get("identityProvider")));
    }

    public Optional<FederatedIdentityLink> findFederatedIdentity(String keycloakUserId, String idpAlias) {
        List<Map<String, Object>> list = withAuth().get()
                .uri("/users/{id}/federated-identity", keycloakUserId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });

        if (list == null) return Optional.empty();

        return list.stream()
                .filter(m -> idpAlias.equals(m.get("identityProvider")))
                .findFirst()
                .map(m -> new FederatedIdentityLink(
                        (String) m.get("identityProvider"),
                        (String) m.get("userId"),     // ← Kakao 'sub'
                        (String) m.get("userName")    // ← Kakao 닉네임
                ));
    }

    // 필요하면 편의 메서드
    public Optional<FederatedIdentityLink> findKakaoIdentity(String keycloakUserId) {
        return findFederatedIdentity(keycloakUserId, "kakao");
    }

    public record FederatedIdentityLink(String identityProvider, String userId, String userName) {
    }
}
