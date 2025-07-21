package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    SUPER_ADMIN,
    TEAM_LEADER,
    TEAM_MEMBER;

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
