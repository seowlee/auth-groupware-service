package pharos.groupware.service.common.util;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    public static final DateTimeFormatter LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); // LocalDateTime 용
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter KST_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(KST);
    private DateUtils() {
    }

    public static String formatKst(OffsetDateTime odt) {
        return odt.atZoneSameInstant(KST).format(LOCAL_FORMATTER);
    }

    /**
     * 입사일 기준 현재 근속 년수 (1년차부터 시작)
     */
    public static int getYearsOfService(LocalDate joinedDate) {
        if (joinedDate == null) return 1;

        LocalDate today = LocalDate.now();
        System.out.println("today: " + today);
        int years = Period.between(joinedDate, today).getYears();
        return Math.max(1, years + 1); // 최소 1년차
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

    public static boolean isWeekend(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }


    public static boolean isHoliday(LocalDate d) {
//        return HolidayRegistry.isHoliday(d);
        return true;
    }

    public static BigDecimal calculateLeaveDays(@NotNull LocalDateTime startDt, @NotNull LocalDateTime endDt) {
        return BigDecimal.ONE;
    }
}
