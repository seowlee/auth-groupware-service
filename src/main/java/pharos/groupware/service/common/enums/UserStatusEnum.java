package pharos.groupware.service.common.enums;

public enum UserStatusEnum {
    ACTIVE,
    INACTIVE,
    PENDING;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
