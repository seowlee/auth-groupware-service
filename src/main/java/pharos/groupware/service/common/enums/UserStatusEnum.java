package pharos.groupware.service.common.enums;

public enum UserStatusEnum {
    ACTIVE,
    INACTIVE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
