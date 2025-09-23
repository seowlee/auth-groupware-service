package pharos.groupware.service.domain.holiday.dto;

import lombok.Builder;
import lombok.Data;
import pharos.groupware.service.domain.holiday.entity.PublicHoliday;

import java.time.LocalDate;

@Data
@Builder
public class CalendarHolidayDto {
    LocalDate date;
    String name;
    @Builder.Default
    boolean publicHoliday = true;
    String type; // optional

    public static CalendarHolidayDto from(PublicHoliday ph) {
        return CalendarHolidayDto.builder()
                .date(ph.getHolidayDate())
                .name(ph.getHolidayName())
                .publicHoliday(true)
                .type("PUBLIC")
                .build();
    }
}
