package pharos.groupware.service.domain.holiday.service;

import pharos.groupware.service.domain.holiday.dto.CalendarHolidayDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface PublicHolidayService {
    Set<LocalDate> dateSetSpanningYears(LocalDate start, LocalDate end);

    List<CalendarHolidayDto> getHolidaysOfYear(int year);

    List<CalendarHolidayDto> getHolidaysBetween(LocalDate start, LocalDate end);
}
