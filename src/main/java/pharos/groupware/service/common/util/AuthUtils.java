package pharos.groupware.service.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthUtils {
    public static String extractUserUUID(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject(); // JWT 로그인 (fallback)
        } else if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("sub"); // Keycloak 로그인
        }
        throw new IllegalStateException("인증 타입을 확인할 수 없습니다.");
    }
}
