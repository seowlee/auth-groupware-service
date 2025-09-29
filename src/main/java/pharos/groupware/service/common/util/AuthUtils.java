package pharos.groupware.service.common.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pharos.groupware.service.common.security.CustomUserDetails;

import java.util.Optional;

public final class AuthUtils {
    private AuthUtils() {
    }

    /**
     * Authentication에서 UUID(문자열)를 시도 추출
     */
    public static Optional<String> tryExtractUserUUID(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.ofNullable(jwtAuth.getToken().getSubject());
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth) {
            return Optional.ofNullable(oauth.getPrincipal().getAttribute("sub"));
        }
        if (authentication instanceof UsernamePasswordAuthenticationToken up) {
            Object p = up.getPrincipal();
            if (p instanceof CustomUserDetails cud && cud.getUserUuid() != null) {
                return Optional.of(cud.getUserUuid().toString());
            }
        }
        return Optional.empty();
    }

    /**
     * SecurityContext에서 바로 추출 + 실패시 예외
     */
    public static String extractUserUUIDOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return tryExtractUserUUID(auth)
                .orElseThrow(() -> new IllegalStateException("인증 정보에서 사용자 UUID를 추출하지 못했습니다."));
    }

    public static String extractUserUUID() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject(); // JWT 로그인 (fallback)
        }
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("sub"); // OIDC(Keycloak)
        }
        if (authentication instanceof UsernamePasswordAuthenticationToken up) { // form login
            Object principal = up.getPrincipal();
            if (principal instanceof CustomUserDetails customUserDetails) {
                return String.valueOf(customUserDetails.getUserUuid());
            }
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
