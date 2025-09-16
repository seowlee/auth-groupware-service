package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum LeaveTypeEnum {
    // 법정
    ANNUAL("연차", "연차(법정)", 15, true, true, null),
    MENSTRUAL_UNPAID("생리휴가", "생리휴가(법정·무급·월1일)", 0, false, false, null),
    FAMILY_CARE_UNPAID("가족돌봄휴가", "가족돌봄휴가(법정·무급·연90일)", 90, false, false, null),
    MATERNITY("출산전후휴가", "출산전후휴가(법정·유급)", 0, false, true, null),
    PATERNITY("배우자출산휴가", "배우자출산휴가(법정·유급·20일)", 20, false, true, null),
    MISCARRIAGE_STILLBIRTH("유산·사산휴가", "유산·사산휴가(법정·유급)", 0, false, true, null),
    PRENATAL_CHECKUP("산전검진시간", "산전검진시간(법정·유급·시간)", 0, false, true, null),

    // 약정
    BIRTHDAY("생일휴가", "생일휴가(약정)", 1, true, true, null),
    SICK("병가", "병가(약정)", 5, true, true, null),   // 필요시 false로
    COMPENSATORY("보상휴가", "보상휴가(약정)", 0, false, true, null),
    OFFICIAL_DUTY("공가/교육", "공가/교육(약정)", 0, false, true, null),
    CUSTOM("기타 휴가", "기타 휴가(약정)", 0, false, false, null),

    // 정산
    ADVANCE("빌려쓴연차", "빌려쓴연차(정산)", 0, false, false, "ANNUAL"),
    BORROWED("차감연차", "차감연차(정산)", 0, false, false, "ANNUAL");

    private final String krName;
    private final String description;
    private final int defaultDays;
    private final boolean initialGrant;
    private final boolean paid;
    private final String parent;

    LeaveTypeEnum(String krName, String description, int defaultDays, boolean initialGrant, boolean paid, String parent) {
        this.krName = krName;
        this.description = description;
        this.defaultDays = defaultDays;
        this.initialGrant = initialGrant;
        this.paid = paid;
        this.parent = parent;
    }

}
