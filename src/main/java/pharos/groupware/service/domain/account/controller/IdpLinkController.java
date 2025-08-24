package pharos.groupware.service.domain.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.account.service.IdpLinkService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Tag(name = "03. 사용자 계정 정보", description = "사용자 계정 IdP 연동 관련 API")
@Controller
@RequiredArgsConstructor
public class IdpLinkController {
    private static final String PROVIDER = "kakao";
    private final KeycloakUserService keycloakUserService;
    private final IdpLinkService idpLinkService;
    private final ObjectMapper om = new ObjectMapper();
    @Value("${keycloak.auth-server-url}")
    private String kc;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;

    @Operation(summary = "사용자: Kakao 연동 시작(리다이렉트)", description = "Kakao 인증 페이지로 이동 후 현재 계정에 IdP 연결합니다")
    @GetMapping("/link/kakao/start")
    public void start(HttpServletResponse res, Authentication authentication, @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client
    ) throws Exception {
        // 1) 현재 로그인 사용자의 ID 토큰에서 session_state 추출
        String sessionState = extractSessionState(authentication, client);
        if (sessionState == null || sessionState.isBlank()) {
            throw new IllegalStateException("session_state를 토큰에서 찾을 수 없습니다. " +
                    "Keycloak 로그인/토큰 클레임 구성을 확인하세요.");
        }

        // 2) nonce & hash 생성 (SHA-256 → base64url, padding 제거)
        String nonce = java.util.UUID.randomUUID().toString();
        String input = nonce + sessionState + clientId + PROVIDER;

        byte[] sha = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
        String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(sha);


        // 3) 링크 완료 후 돌아올 앱 주소 (Keycloak 클라이언트의 Redirect URIs에 반드시 포함)
        String redirectUri = URLEncoder.encode(
                "http://localhost:8081/link/kakao/callback",
                StandardCharsets.UTF_8
        );

        // 4) 최종 링크 URL로 리다이렉트
        String url = String.format(
                "%s/realms/%s/broker/%s/link?client_id=%s&redirect_uri=%s&nonce=%s&hash=%s",
                kc, realm, PROVIDER, clientId, redirectUri, nonce, hash
        );
        res.sendRedirect(url);
    }

    // 옵션: 완료 콜백(성공/실패 표시용)
    @GetMapping("/link/kakao/callback")
    public String callback(RedirectAttributes ra) {
        String keycloakUserId = AuthUtils.extractUserUUID();
        try {
            idpLinkService.persistKakaoSub(keycloakUserId);
            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastMsg", "카카오 계정이 연동되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastMsg", "카카오 연동 실패: " + e.getMessage());
        }
        return "redirect:/home?linkedKakao=1";
    }

    private String extractSessionState(Authentication authentication, OAuth2AuthorizedClient client) {
        // 1) OIDC ID 토큰에서 시도
        if (authentication instanceof OAuth2AuthenticationToken oat && oat.getPrincipal() instanceof OidcUser oidc) {
            String fromIdToken = oidc.getIdToken().getClaimAsString("session_state");
            if (fromIdToken != null && !fromIdToken.isBlank()) return fromIdToken;
            Object claim = oidc.getClaims().get("session_state");
            if (claim != null) return claim.toString();
        }

        // 2) 액세스 토큰(JWT) 페이로드 디코드해서 시도
        if (client != null && client.getAccessToken() != null) {
            String token = client.getAccessToken().getTokenValue(); // "AAA.BBB.CCC"
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                try {
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    JsonNode node = om.readTree(payloadJson);
                    if (node.has("session_state")) return node.get("session_state").asText();
                    // 환경에 따라 sid 만 있을 수도 있으니 보조로 지원 (링크 해시엔 session_state가 필요)
//                    System.out.println("case4========");
                    if (node.has("sid")) return node.get("sid").asText();
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }
}

