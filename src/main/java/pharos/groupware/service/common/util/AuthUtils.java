package pharos.groupware.service.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return "system";

        Object principal = authentication.getPrincipal();
        // OIDC 로그인 사용자
        if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }

        // JWT 토큰 기반 로그인 사용자
        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }

        // 일반 UserDetails 로그인 (예: form 로그인)
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        // 그 외 fallback
        return principal.toString();
    }
}
