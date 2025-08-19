package pharos.groupware.service.common.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoleCheckAspect {
    private final UserRepository userRepository;

    @Before("@annotation(pharos.groupware.service.common.annotation.RequireSuperAdmin) && args(..)")
    public void checkSuperAdmin() {
        checkRole(UserRoleEnum.SUPER_ADMIN);
    }

    @Before("@annotation(pharos.groupware.service.common.annotation.RequireTeamLeader) && args(..)")
    public void checkTeamLeader() {
        checkRole(UserRoleEnum.TEAM_LEADER);
    }

    private void checkRole(UserRoleEnum requiredRole) {
        String uuid = AuthUtils.extractUserUUID();
        User user = userRepository.findByUserUuid(UUID.fromString(uuid))
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if (user.getRole() != requiredRole) {
            throw new AccessDeniedException(requiredRole + " 권한이 필요합니다.");
        }
    }
}
