package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    PENDING("대기");
    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }


    public boolean isActive() {
        return this == ACTIVE;
    }
}
