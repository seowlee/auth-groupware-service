package pharos.groupware.service.infrastructure.graph;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.common.util.DateUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GraphGroupService {

    private final RestClient.Builder restClientBuilder;
    private final GraphDelegatedTokenProvider graphDelegatedTokenProvider;

    @Value("${graph.base-url}")
    private String baseUrl;

    @Value("${graph.timezone}")
    private String timezone;

    @Value("${graph.calendar.group-id}")
    private String groupId;

    private RestClient withDelegatedToken() {
        String accessToken = graphDelegatedTokenProvider.getAccessToken();
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    // 그룹 이벤트 생성
    public String createGroupEvent(
            String subject, String body,
            LocalDateTime start, LocalDateTime end
    ) {
        Map<String, Object> event = Map.of(
                "subject", subject,
                "body", Map.of("contentType", "Text", "content", body),
                "start", Map.of("dateTime", start.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "end", Map.of("dateTime", end.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "location", Map.of("displayName", "휴가")
        );

        Map<String, Object> res = withDelegatedToken().post()
                // 권장 경로: /groups/{id}/events
                .uri("/groups/{groupId}/events", groupId)
                .body(event)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return res == null ? null : Objects.toString(res.get("id"), null);
    }

    // 그룹 이벤트 목록
    public Map<String, Object> listGroupEvents() {
        return withDelegatedToken().get()
                .uri("/groups/{groupId}/events", groupId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    // 그룹 이벤트 수정
    public void updateGroupEvent(String eventId,
                                 String subject, String body,
                                 LocalDateTime start, LocalDateTime end) {
        Map<String, Object> patch = Map.of(
                "subject", subject,
                "body", Map.of("contentType", "Text", "content", body),
                "start", Map.of("dateTime", start.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone),
                "end", Map.of("dateTime", end.format(DateUtils.LOCAL_FORMATTER), "timeZone", timezone)
        );
        withDelegatedToken().patch()
                .uri("/groups/{groupId}/events/{eventId}", groupId, eventId)
                .body(patch)
                .retrieve()
                .toBodilessEntity();
    }

    // 그룹 이벤트 삭제
    public void deleteGroupEvent(String eventId) {
        withDelegatedToken().delete()
                .uri("/groups/{groupId}/events/{eventId}", groupId, eventId)
                .retrieve()
                .toBodilessEntity();
    }
}
