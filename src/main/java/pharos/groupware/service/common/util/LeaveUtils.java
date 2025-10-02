package pharos.groupware.service.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Set;

public class LeaveUtils {
    private static final LocalTime BIZ_START = LocalTime.of(9, 0);
    private static final LocalTime BIZ_END = LocalTime.of(17, 0);

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

    // LeaveUtils.java
    public static BigDecimal scale(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        return v.setScale(3, RoundingMode.DOWN);
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

    /**
     * 연차 사용일 계산: 시작일~종료일 사이에서 주말/공휴일 제외
     */
    public static BigDecimal calculateLeaveDays(LocalDateTime startDt, LocalDateTime endDt, Set<LocalDate> holidays) {
        if (startDt == null || endDt == null)
            throw new IllegalArgumentException("시작/종료 일시가 필요합니다.");
        if (endDt.isBefore(startDt))
            throw new IllegalArgumentException("종료일이 시작일보다 앞설 수 없습니다.");

        long totalMinutes = 0;
        LocalDate startDate = startDt.toLocalDate();
        LocalDate endDate = endDt.toLocalDate();
        LocalDate d = startDate;

        while (!d.isAfter(endDate)) {
            // 주말/공휴일 skip
            if (!DateUtils.isWeekend(d) && !DateUtils.isHoliday(d, holidays)) {
                // 그 날의 업무시간 창
                LocalDateTime dayStart = LocalDateTime.of(d, BIZ_START);
                LocalDateTime dayEnd = LocalDateTime.of(d, BIZ_END);

                // 실제 겹치는 구간 = [max(dayStart, startDt), min(dayEnd, endDt)]
                LocalDateTime from = DateUtils.max(startDt, dayStart);
                LocalDateTime to = DateUtils.min(endDt, dayEnd);

                if (!to.isBefore(from)) {
                    long minutes = Duration.between(from, to).toMinutes();
                    // 음수 방지 및 0분 처리
                    if (minutes > 0) totalMinutes += minutes;
                }
            }
            d = d.plusDays(1);
        }
        // 8시간(480분) = 1일
        BigDecimal days = new BigDecimal(totalMinutes)
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)   // 분→시간
                .divide(BigDecimal.valueOf(8), 3, RoundingMode.HALF_UP);   // 시간→일

        // 음수/NaN 방지
        if (days.signum() < 0) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        return days.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * 근속 k년차의 기간 [from, to] 계산 (KST, from=해당년 00:00:00, to=다음해 00:00:00 -1s)
     */
    public static Window computeTenureWindow(LocalDate joined, int k) {
        if (joined == null) throw new IllegalArgumentException("joinedDate is null");
        if (k <= 0) throw new IllegalArgumentException("k must be >= 1");

        OffsetDateTime from = joined.plusYears(k - 1L)
                .atStartOfDay(DateUtils.KST).toOffsetDateTime();
        OffsetDateTime to = joined.plusYears(k)
                .atStartOfDay(DateUtils.KST).toOffsetDateTime()
                .minusSeconds(1);
        return new Window(from, to);
    }

    /**
     * UI 라벨: 예) "근속 2년차 (2025.02.26 ~ 2026.02.25)"
     */
    public static String tenureLabel(LocalDate joined, int currentYearNumber, int k) {
        Window w = computeTenureWindow(joined, k);
        String head = (k == currentYearNumber) ? ("근속 " + k + "년차") : (k + "년차");
        return head + " (" + DateUtils.fmtYmd(w.from()) + " ~ " + DateUtils.fmtYmd(w.to()) + ")";
    }

    /**
     * 결과 컨테이너 (record 대신 클래스로)
     */
    public static final class Window {
        private final OffsetDateTime from;
        private final OffsetDateTime to;

        public Window(OffsetDateTime from, OffsetDateTime to) {
            this.from = from;
            this.to = to;
        }

        public OffsetDateTime from() {
            return from;
        }

        public OffsetDateTime to() {
            return to;
        }
    }
}
