package pharos.groupware.service.common.security;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.util.UUID;

@Component
@RequestScope
@RequiredArgsConstructor
public class CurrentActorProvider {

    private final UserRepository userRepository;

    private AppUser cached; // 요청 단위 캐시

    public AppUser get() {
        if (cached != null) return cached;

        // JWT/OAuth2 어느 쪽이든 sub 추출
        String sub = AuthUtils.extractUserUUID();
        if (sub == null || sub.isBlank()) {
            // 401로 떨어지도록 인증 부족 예외 던짐
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new InsufficientAuthenticationException("유효하지 않은 사용자 식별자입니다.");
        }
        User u = userRepository.findByUserUuid(UUID.fromString(sub))
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자를 찾을 수 없습니다."));

        cached = new AppUser(u.getId(), u.getUserUuid(), u.getUsername(), u.getRole(), u.getTeamId());
        return cached;
    }
}