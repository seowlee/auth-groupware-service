package pharos.groupware.service.common.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

public class LeaveUtils {
    private LeaveUtils() {
    }

    /**
     * 입사일 기준 근속연차(1부터)
     */
    public static int getCurrentYearNumber(LocalDate joinedDate) {
        if (joinedDate == null) return 1;

        LocalDate today = LocalDate.now();
        System.out.println("today: " + today);
        int years = Period.between(joinedDate, today).getYears();
        return Math.max(1, years + 1);
    }

    public static int annualGrantDays(int yearNumber) {
        if (yearNumber <= 1) return 0;
        int add = (yearNumber - 1) / 2;
        return Math.min(25, 15 + add);
    }

    public static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 이번 달이 입사 ‘기념월’인지 (배치 대상 필터에 사용)
     */
    public static boolean isAnniversaryMonth(LocalDate joinedDate) {
        LocalDate today = LocalDate.now();

        if (joinedDate == null) return false;
        return joinedDate.getMonth() == today.getMonth();
    }

    /**
     * 오늘이 '입사 기념일'인지 (월/일 기준, 윤년 2/29 입사자는 평년엔 2/28로 간주)
     */
    public static boolean isAnniversaryDay(LocalDate joinedDate) {
        if (joinedDate == null) return false;

        LocalDate today = LocalDate.now();
        // 2/29 보정
        int m = joinedDate.getMonthValue();
        int d = joinedDate.getDayOfMonth();
        if (m == 2 && d == 29 && !today.isLeapYear()) {
            return today.getMonthValue() == 2 && today.getDayOfMonth() == 28;
        }
        return today.getMonthValue() == m && today.getDayOfMonth() == d;
    }
}
