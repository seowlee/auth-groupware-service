package pharos.groupware.service.common.security;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
        User u = userRepository.findByUserUuid(UUID.fromString(sub))
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자를 찾을 수 없습니다."));

        cached = new AppUser(u.getId(), u.getUserUuid(), u.getUsername(), u.getRole());
        return cached;
    }
}