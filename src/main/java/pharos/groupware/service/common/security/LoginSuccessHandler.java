package pharos.groupware.service.common.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import pharos.groupware.service.common.session.SessionKeys;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @PostConstruct
    void init() {
        // SavedRequest가 있으면 그리로, 없으면 /home
        setDefaultTargetUrl("/home");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        // 1) UUID 안전 추출
        String uuidStr = AuthUtils.tryExtractUserUUID(authentication)
                .orElse(null);

        if (uuidStr == null) {
            // 세션에 에러 메시지 세팅 후 로그인 페이지로
            request.getSession().setAttribute("errorMessage", "로그인 사용자 UUID를 찾을 수 없습니다.");
            getRedirectStrategy().sendRedirect(request, response, "/login?error");
            return;
        }

        // 2) DB 사용자 조회 (없으면 정책대로 처리: 가입 대기/차단 등)
        final UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("errorMessage", "잘못된 사용자 식별자 형식입니다.");
            getRedirectStrategy().sendRedirect(request, response, "/login?error");
            return;
        }

        User u = userRepository.findByUserUuid(uuid).orElse(null);
        if (u == null) {
            // 필요시 자동 생성 로직을 넣거나, 승인 대기 페이지로 보냄
            getRedirectStrategy().sendRedirect(request, response, "/error/pending-approval");
            return;
        }

        // 3) 세션에 AppUser 저장 (UI에서 가볍게 사용)
        request.getSession().setAttribute(
                SessionKeys.CURRENT_USER,
                new AppUser(u.getId(), u.getUserUuid(), u.getUsername(), u.getRole(), u.getTeamId())
        );
        // 4) 의심 SavedRequest 가드: DevTools/파비콘/continue 유발 리다이렉트는 무시하고 /home
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        SavedRequest saved = cache.getRequest(request, response);
        if (saved != null) {
            String url = saved.getRedirectUrl();
            if (url != null && (
                    url.contains("/.well-known/") ||
                            url.contains("/favicon.ico") ||
                            url.matches(".*[?&]continue(?:=|$).*")
            )) {
                cache.removeRequest(request, response);
                getRedirectStrategy().sendRedirect(request, response, "/home");
                return;
            }
        }
        // 5) 정상 플로우: SavedRequest가 정상이면 원래 가려던 URL로 이동(없으면 /home)
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
