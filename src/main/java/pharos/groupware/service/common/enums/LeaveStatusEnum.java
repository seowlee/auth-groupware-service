package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum LeaveStatusEnum {
    APPROVED("승인완료"),
    CANCELED("취소"),
    REJECTED("반려"),
    PENDING("대기");

    private final String description;

    LeaveStatusEnum(String description) {
        this.description = description;
    }

}
