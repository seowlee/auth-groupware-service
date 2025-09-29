package pharos.groupware.service.domain.admin.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.common.session.SessionKeys;
import pharos.groupware.service.domain.m365.dto.OAuthTokenResDto;
import pharos.groupware.service.domain.m365.dto.SaveDelegatedTokenCommandDto;
import pharos.groupware.service.domain.m365.service.M365TokenStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequestMapping("/admin/m365")
@RequiredArgsConstructor
public class M365AuthController {

    private final RestClient.Builder restClientBuilder;
    private final M365TokenStore tokenStore;
    @Value("${graph.client-id}")
    String clientId;
    @Value("${graph.client-secret}")
    String clientSecret;  // 서버측 코드 플로우 사용
    @Value("${graph.tenant-id}")
    String tenantId;
    @Value("${graph.redirect-uri}")
    String redirectUri;   // e.g. https://host/admin/m365/callback
    @Value("${graph.token-uri}")
    String tokenUri;      // https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token
    @Value("${graph.authorize-uri}")
    String authorizeUri;  // https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize
    @Value("${graph.delegated-scope}")
    String delegatedScope;

    private RestClient tokenClient() {
        return restClientBuilder.baseUrl(tokenUri).build();
    }

    @GetMapping("/connect")
    public void connect(HttpServletResponse resp, HttpSession session) throws IOException {
        // CSRF 방지용 state
        String state = UUID.randomUUID().toString();
        session.setAttribute("m365_oauth_state", state);

        String url = UriComponentsBuilder
                .fromUriString(authorizeUri)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_mode", "query")
                .queryParam("scope", delegatedScope)
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        resp.sendRedirect(url);
    }

    @GetMapping("/callback")
    public String callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session,
            RedirectAttributes ra) {

        // 1) 기본 파라미터 검증
        if (code == null || code.isBlank()) {
            ra.addFlashAttribute("error", "인증 코드가 없습니다.");
            return "redirect:/home";
        }

        // 2) CSRF(state) 검증
        String expectedState = (String) session.getAttribute("m365_oauth_state");
        session.removeAttribute("m365_oauth_state");
        if (expectedState != null && !expectedState.equals(state)) {
            ra.addFlashAttribute("error", "state 불일치로 요청이 거부되었습니다.");
            return "redirect:/home";
        }

        // 3) 토큰 교환
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        OAuthTokenResDto res = tokenClient().post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    String msg = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("토큰 교환 실패(" + resp.getStatusCode() + "): " + msg);
                })
                .body(OAuthTokenResDto.class);

        // 4) 응답/리프레시 토큰 검증 (한 번에 처리)
        if (res == null || res.getRefreshToken() == null || res.getRefreshToken().isBlank()) {
            ra.addFlashAttribute("error", "Microsoft 365 연동 실패: refresh_token 없음.");
            return "redirect:/home";
        }

        // 5) 세션의 AppUser에서 감사 정보 추출
        AppUser au = (AppUser) session.getAttribute(SessionKeys.CURRENT_USER);
        String byUuid = (au != null && au.userUuid() != null) ? au.userUuid().toString() : null;
        String byName = (au != null) ? au.username() : null;

        // 6) 저장
        tokenStore.saveDelegatedToken(SaveDelegatedTokenCommandDto.builder()
                .refreshTokenPlain(res.getRefreshToken())
                .tenantId(tenantId)                 // 환경에서 주입받은 값
                .clientId(clientId)                 // 환경에서 주입받은 값
                .scope(res.getScope())              // 응답에 포함되면 기록
                .byUuid(byUuid)
                .byName(byName)
                .build());

        ra.addFlashAttribute("msg", "Microsoft 365 연동이 완료되었습니다.");
        return "redirect:/home";
    }
}

