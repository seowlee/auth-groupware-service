package pharos.groupware.service.infrastructure.graph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.team.dto.CreateCalendarEventReqDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class GraphUserService {
    private final RestClient restClient;

    public GraphUserService(
            GraphTokenProvider tokenProvider,
            RestClient.Builder builder,
            @Value("${graph.base-url}") String baseUrl
    ) {
        String token = tokenProvider.getAccessToken();
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    // 특정 사용자 조회
    public Map<String, Object> getUser(String userId) {
        return restClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public String createUser(CreateUserReqDto reqDto) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("accountEnabled", true);
        userData.put("displayName", reqDto.getFirstName() + " " + reqDto.getLastName());
        userData.put("mailNickname", reqDto.getUsername());
        userData.put("userPrincipalName", reqDto.getUsername() + "@gwco.onmicrosoft.com");
        userData.put("usageLocation", "KR");
        userData.put("passwordProfile", Map.of(
                "forceChangePasswordNextSignIn", false,
                "password", reqDto.getRawPassword()
        ));

        // 본문으로부터 id 추출
        Map<String, Object> responseBody = restClient.post()
                .uri("/users")
                .body(userData)
                .retrieve()
                .body(Map.class);
        System.out.println("userId: " + responseBody.get("id").toString());
//        assignLicenseToUser(userId);

        return responseBody.get("id").toString();
    }

    public void assignLicenseToUser(String userId) {
        String skuId = "f245ecc8-75af-4f8e-b61f-27d8114de5f3"; // Business Standard

        Map<String, Object> requestBody = Map.of(
                "addLicenses", List.of(Map.of("skuId", skuId)),
                "removeLicenses", List.of()
        );

        restClient.post()
                .uri("/users/{id}/assignLicense", userId)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }


    public Map<String, Object> listUsers() {
        return restClient.get()
                .uri("/users")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public Map<String, Object> listEvents(String userId) {
        return restClient.get()
                .uri("/users/{userId}/calendar/events", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public void createEvent(CreateCalendarEventReqDto dto) {
        Map<String, Object> eventData = Map.of(
                "subject", dto.getSubject(),
                "body", Map.of("contentType", "Text", "content", dto.getBodyContent()),
                "start", Map.of("dateTime", dto.getStartDateTime(), "timeZone", dto.getTimezone()),
                "end", Map.of("dateTime", dto.getEndDateTime(), "timeZone", dto.getTimezone())
        );
        restClient.post()
                .uri("/users/{userId}/events", dto.getGraphUserId())
                .body(eventData)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteEvent(String userId, String eventId) {
        restClient.delete()
                .uri("/users/{userId}/calendar/events/{eventId}", userId, eventId)
                .retrieve()
                .toBodilessEntity();
    }
}

