package pharos.groupware.service.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pharos.groupware.service.team.domain.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.secret-key}")
    private String secretKeyStr;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidityInMs;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityInMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKeyStr.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        return createToken(user.getUserUuid().toString(), user.getRole().name(), accessTokenValidityInMs);
    }

    public String createRefreshToken(User user) {
        return createToken(user.getUserUuid().toString(), user.getRole().name(), refreshTokenValidityInMs);
    }

    private String createToken(String username, String role, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // 클레임 추출
    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
