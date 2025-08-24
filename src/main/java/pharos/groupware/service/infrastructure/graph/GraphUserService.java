package pharos.groupware.service.infrastructure.graph;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class GraphUserService {
    private final GraphTokenProvider graphTokenProvider;
    private final RestClient.Builder restClientBuilder;
    private final String baseUrl;
    private final String graphUserId;
    private final String timezone;
//    private final RestClient restClient;

    public GraphUserService(
            GraphTokenProvider tokenProvider,
            RestClient.Builder builder,
            @Value("${graph.base-url}") String baseUrl,
            @Value("${graph.calendar-user-id}") String graphUserId,
            @Value("${graph.timezone}") String timezone
    ) {
        this.graphTokenProvider = tokenProvider;
        this.restClientBuilder = builder;
        this.baseUrl = baseUrl;
        this.graphUserId = graphUserId;
        this.timezone = timezone;
    }

    private RestClient withAuth() {
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + graphTokenProvider.getAccessToken())
                .build();
    }

    // 특정 사용자 조회
    public Map<String, Object> getUser(String userId) {
        return withAuth().get()
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
        Map<String, Object> responseBody = withAuth().post()
                .uri("/users")
                .body(userData)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
//        assignLicenseToUser(userId);

        assert responseBody != null;
        return responseBody.get("id").toString();
    }

    public void deleteUser(String userId) {
        withAuth().delete()
                .uri("/users/{id}", userId)
                .retrieve()
                .toBodilessEntity();
    }

    public void assignLicenseToUser(String userId) {
        String skuId = "f245ecc8-75af-4f8e-b61f-27d8114de5f3"; // Business Standard

        Map<String, Object> requestBody = Map.of(
                "addLicenses", List.of(Map.of("skuId", skuId)),
                "removeLicenses", List.of()
        );

        withAuth().post()
                .uri("/users/{id}/assignLicense", userId)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }


    public Map<String, Object> listUsers() {
        return withAuth().get()
                .uri("/users")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public Map<String, Object> listEvents(String userId) {
        return withAuth().get()
                .uri("/users/{userId}/calendar/events", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public String createEvent(
            String subject,
            String body,
            LocalDateTime start,
            LocalDateTime end) {

        Map<String, Object> eventData = Map.of(
                "subject", subject,
                "body", Map.of("contentType", "Text", "content", body),
                "start", Map.of("dateTime", start.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "end", Map.of("dateTime", end.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "location", Map.of("displayName", "휴가")
        );
        Map<String, Object> response = withAuth().post()
                .uri("/users/{userId}/events", this.graphUserId)
                .body(eventData)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assert response != null;
        return Objects.toString(response.get("id"), null);
    }

    public void updateEvent(String calendarEventId, String subject, String body, LocalDateTime startDt, LocalDateTime endDt) {
        Map<String, Object> eventData = Map.of(
                "subject", subject,
                "body", Map.of("contentType", "Text", "content", body),
                "start", Map.of("dateTime", startDt.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "end", Map.of("dateTime", endDt.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "location", Map.of("displayName", "휴가")
        );
        withAuth().patch()
                .uri("/users/{userId}/events/{id}", this.graphUserId, calendarEventId)
                .body(eventData)
                .retrieve()
                .toBodilessEntity();

        log.info("Graph event updated. id={}", calendarEventId);


    }

    public void deleteEvent(String eventId) {
        withAuth().delete()
                .uri("/users/{userId}/calendar/events/{eventId}", this.graphUserId, eventId)
                .retrieve()
                .toBodilessEntity();
    }


}

