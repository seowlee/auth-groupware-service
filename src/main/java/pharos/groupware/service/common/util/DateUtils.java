package pharos.groupware.service.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public final class DateUtils {
    public static final DateTimeFormatter LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); // LocalDateTime 용
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter KST_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(KST);

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


    public static LocalDate parseToDate(String s, boolean startOfDay) {
        try {
            return OffsetDateTime.parse(s).toLocalDate();
        } catch (Exception ignore) {
            return LocalDate.parse(s);
        }
    }

    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return (a.isAfter(b)) ? a : b;
    }

    public static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return (a.isBefore(b)) ? a : b;
    }

    // 필요시: 하루 전부 쉬는지(주말/공휴 제외하고 09~17 꽉 채우면 true)
    public static boolean isFullBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        return !isWeekend(date) && !isHoliday(date, holidays);
    }

}
