package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    SUPER_ADMIN("최고관리자"),
    TEAM_LEADER("팀장"),
    TEAM_MEMBER("팀원");
    private final String description;

    UserRoleEnum(String description) {
        this.description = description;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    public boolean isTeamLeader() {
        return this == TEAM_LEADER;
    }
}
