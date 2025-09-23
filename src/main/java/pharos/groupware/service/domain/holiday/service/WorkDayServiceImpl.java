package pharos.groupware.service.domain.holiday.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.domain.holiday.entity.PublicHolidayRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkDayServiceImpl implements WorkDayService {
    private final PublicHolidayRepository publicHolidayRepository;
    private final PublicHolidayService holidayService;
    /**
     * 주말/공휴일 제외 근무일 여부
     */
//    @Transactional(readOnly = true)
//    public boolean isWorkDay(LocalDate date) {
//        Set<LocalDate> holidays = holidaysFor(startDt.toLocalDate(), endDt.toLocalDate());
//        return !isWeekend(date) && !isHoliday(date, holidays);
//    }

    /**
     * 기간에 필요한 공휴일 세트(연도 걸침 대응)
     */
    @Override
    @Transactional(readOnly = true)
    public Set<LocalDate> holidaysFor(LocalDate start, LocalDate end) {
//        Set<LocalDate> out = new HashSet<>();
//        int y1 = start.getYear(), y2 = end.getYear();
//        for (int y = y1; y <= y2; y++) {
//            publicHolidayRepository.findAllByYear(y)
//                    .stream().map(PublicHoliday::getHolidayDate)
//                    .forEach(out::add);
//        }
//        return out;
        return holidayService.dateSetSpanningYears(start, end);
    }

    /**
     * 주말/공휴일 제외 사용일 계산(실제 계산은 DateUtils에 위임)
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal countLeaveDays(LocalDateTime startDt, LocalDateTime endDt) {
        Set<LocalDate> holidays = holidaysFor(startDt.toLocalDate(), endDt.toLocalDate());
        return LeaveUtils.calculateLeaveDays(startDt, endDt, holidays);
    }

    /**
     * 선택 사항: 명시적으로 금지 사유를 알려주고 싶을 때
     */
    @Override
    @Transactional(readOnly = true)
    public void assertBusinessRange(LocalDateTime startDt, LocalDateTime endDt) {
        BigDecimal used = countLeaveDays(startDt, endDt);
        if (used.signum() <= 0) {
            throw new IllegalArgumentException("주말/공휴일만 포함하거나 근무시간 슬럿이 유효하지 않습니다.");
        }
    }
}
