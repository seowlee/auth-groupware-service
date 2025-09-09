package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum LeaveTypeEnum {
    // 법정
    ANNUAL("연차(법정)", 15, true, true),
    MENSTRUAL_UNPAID("생리휴가(법정·무급·월1일)", 0, false, false),
    FAMILY_CARE_UNPAID("가족돌봄휴가(법정·무급·연90일)", 0, false, false),
    MATERNITY("출산전후휴가(법정·유급)", 0, false, true),
    PATERNITY("배우자출산휴가(법정·유급·20일)", 0, false, true),
    MISCARRIAGE_STILLBIRTH("유산·사산휴가(법정·유급)", 0, false, true),
    PRENATAL_CHECKUP("산전검진시간(법정·유급·시간)", 0, false, true),

    // 약정
    BIRTHDAY("생일연차(약정)", 1, true, true),
    SICK("병가(약정)", 5, true, true),   // 필요시 false로
    COMPENSATORY("보상휴가(약정)", 0, false, true),
    OFFICIAL_DUTY("공가/교육(약정)", 0, false, true),
    CUSTOM("기타 휴가(약정)", 0, false, false),

    // 정산
    ADVANCE("빌려쓴연차(정산)", 0, false, false),
    BORROWED("차감연차(정산)", 0, false, false);

    //    private final String name;
    private final String description;
    private final int defaultDays;
    private final boolean initialGrant;
    private final boolean paid;

    LeaveTypeEnum(String description, int defaultDays, boolean initialGrant, boolean paid) {
        this.description = description;
        this.defaultDays = defaultDays;
        this.initialGrant = initialGrant;
        this.paid = paid;
    }

}
