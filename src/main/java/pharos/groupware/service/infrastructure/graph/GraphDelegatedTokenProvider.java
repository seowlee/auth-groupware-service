package pharos.groupware.service.infrastructure.graph;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import pharos.groupware.service.domain.m365.dto.OAuthTokenResDto;
import pharos.groupware.service.domain.m365.service.M365TokenStore;

import java.nio.charset.StandardCharsets;

@Component
public class GraphDelegatedTokenProvider {
    private final RestClient tokenClient;
    private final String clientId;
    private final String clientSecret;
    private final M365TokenStore tokenStore;

    public GraphDelegatedTokenProvider(
            RestClient.Builder builder,
            @Qualifier("dbM365TokenStore") M365TokenStore tokenStore,
            @Value("${graph.token-uri}") String tokenUri,
            @Value("${graph.client-id}") String clientId,
            @Value("${graph.client-secret}") String clientSecret
    ) {
        this.tokenClient = builder.baseUrl(tokenUri).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenStore = tokenStore;
    }

    // NEW: 항상 유효한 access_token을 반환 (만료 시 refresh)
    public String getAccessToken() {
        String refreshToken = tokenStore.loadRefreshToken()
                .orElseThrow(() -> new IllegalStateException("Microsoft 365 연동이 아직 완료되지 않았습니다. (refresh token 없음)"));

        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        // delegated 권한 범위. 최소필요 권한만 포함
        body.add("scope", "openid profile offline_access Group.ReadWrite.All");
        body.add("refresh_token", refreshToken);

        OAuthTokenResDto res = tokenClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    String msg = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("토큰 갱신 실패(" + resp.getStatusCode() + "): " + msg);
                })
                .body(OAuthTokenResDto.class);

        if (res == null || res.getAccessToken() == null || res.getAccessToken().isBlank()) {
            throw new IllegalStateException("access_token 발급 실패");
        }
        // RT 회전(rotation) 대응: 새 RT가 오면 저장
        if (res.getRefreshToken() != null && !res.getRefreshToken().isBlank()) {
            tokenStore.saveRefreshToken(res.getRefreshToken());
        }
        return res.getAccessToken();
    }
}
