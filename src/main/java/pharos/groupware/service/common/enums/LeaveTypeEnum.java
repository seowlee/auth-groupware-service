package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum LeaveTypeEnum {
    ANNUAL("연차", 15, true),
    BIRTHDAY("생일연차", 1, true),
    SICK("병가", 5, true),
    ADVANCE("땡겨쓴 연차", 0, false),
    BORROWED("차감 연차", 0, false),
    CUSTOM("기타 휴가", 0, false);

    private final String description;
    private final int defaultDays;
    private final boolean initialGrant;

    LeaveTypeEnum(String description, int defaultDays, boolean initialGrant) {
        this.description = description;
        this.defaultDays = defaultDays;
        this.initialGrant = initialGrant;
    }
    
}
