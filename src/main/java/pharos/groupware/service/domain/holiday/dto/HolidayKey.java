package pharos.groupware.service.domain.holiday.dto;

import java.time.LocalDate;

public record HolidayKey(LocalDate date, short seq) {
}
