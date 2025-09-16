package pharos.groupware.service.domain.holiday.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public interface WorkDayService {
    Set<LocalDate> holidaysFor(LocalDate start, LocalDate end);

    BigDecimal countLeaveDays(LocalDateTime startDt, LocalDateTime endDt);

    void assertBusinessRange(LocalDateTime startDt, LocalDateTime endDt);

}
