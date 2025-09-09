package pharos.groupware.service.common.util;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public final class DateUtils {
    public static final DateTimeFormatter LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); // LocalDateTime 용
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter KST_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(KST);
    private static final LocalTime BIZ_START = LocalTime.of(9, 0);
    private static final LocalTime BIZ_END = LocalTime.of(17, 0);

    private DateUtils() {
    }

    public static String formatKst(OffsetDateTime odt) {
        return odt.atZoneSameInstant(KST).format(LOCAL_FORMATTER);
    }


    /**
     * LocalDateTime(타임존 없음)을 서울 시간 기준의 OffsetDateTime으로 변환합니다.
     *
     * @param localDateTime 변환할 LocalDateTime 객체
     * @return 서울 시간 기준 OffsetDateTime 객체
     */
    public static OffsetDateTime toSeoulOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(KST).toOffsetDateTime();
    }

    public static boolean isTodayWeekend() {
        return isWeekend(LocalDate.now());
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }


    public static boolean isHoliday(LocalDate date, Set<LocalDate> holidays) {
        return holidays.contains(date);
    }

    public static BigDecimal calculateLeaveDays(@NotNull LocalDateTime startDt, @NotNull LocalDateTime endDt) {
        return BigDecimal.ONE;
    }

    /**
     * 연차 사용일 계산: 시작일~종료일 사이에서 주말/공휴일 제외
     */
    public static BigDecimal countLeaveDays(LocalDateTime startDt, LocalDateTime endDt, Set<LocalDate> holidays) {
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
            if (!isWeekend(d) && !isHoliday(d, holidays)) {
                // 그 날의 업무시간 창
                LocalDateTime dayStart = LocalDateTime.of(d, BIZ_START);
                LocalDateTime dayEnd = LocalDateTime.of(d, BIZ_END);

                // 실제 겹치는 구간 = [max(dayStart, startDt), min(dayEnd, endDt)]
                LocalDateTime from = max(startDt, dayStart);
                LocalDateTime to = min(endDt, dayEnd);

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
    }    // 보조: 두 LocalDateTime 중 늦은/이른 값 선택

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return (a.isAfter(b)) ? a : b;
    }

    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return (a.isBefore(b)) ? a : b;
    }

    // 필요시: 하루 전부 쉬는지(주말/공휴 제외하고 09~17 꽉 채우면 true)
    public static boolean isFullBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        return !isWeekend(date) && !isHoliday(date, holidays);
    }

}
