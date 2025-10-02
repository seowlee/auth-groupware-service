package pharos.groupware.service.domain.view.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.domain.team.entity.User;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaveListFilterViewDto(
        String username,
        String role,          // "SUPER_ADMIN" | "TEAM_LEADER" | "MEMBER" | "ANONYMOUS"
        boolean superAdmin,
        boolean teamLeader,
        Long teamId,
        Integer yearNumber,
        LocalDate joinedDate
) {
    /**
     * actor(세션) + user(DB)로 화면 필터 컨텍스트를 만듭니다.
     */
    public static LeaveListFilterViewDto from(AppUser actor, User user) {
        if (actor == null) {
            return new LeaveListFilterViewDto(null, "ANONYMOUS", false, false, null, 1, null);
        }
        String role = actor.role() != null ? actor.role().name() : "MEMBER";
        boolean isSuper = actor.role() != null && actor.role().isSuperAdmin();
        boolean isLeader = actor.role() != null && actor.role().isTeamLeader(); // 프로젝트 규칙에 맞게

        Long teamId = (user != null && user.getTeam() != null) ? user.getTeam().getId() : null;
        Integer year = (user != null) ? user.getYearNumber() : 1;
        LocalDate jd = (user != null) ? user.getJoinedDate() : null;

        return new LeaveListFilterViewDto(actor.username(), role, isSuper, isLeader, teamId, year, jd);
    }

    /* (선택) 뷰에서 쓰기 좋은 헬퍼들 */
    public boolean isMemberOnly() {
        return !superAdmin && !teamLeader;
    }

    public boolean canSeeAllUsers() {
        return superAdmin;
    }

    public boolean canSeeTeam() {
        return teamLeader || superAdmin;
    }
}