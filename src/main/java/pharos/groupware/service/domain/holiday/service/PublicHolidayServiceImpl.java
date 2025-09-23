package pharos.groupware.service.domain.holiday.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pharos.groupware.service.domain.holiday.dto.CalendarHolidayDto;
import pharos.groupware.service.domain.holiday.entity.PublicHolidayRepository;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHolidayServiceImpl implements PublicHolidayService {
    private final PublicHolidayRepository publicHolidayRepository;

    // 연 단위 캐시
    @Override
    @Cacheable(value = "holidaysByYear", key = "#year")
    public List<CalendarHolidayDto> getHolidaysOfYear(int year) {
        return publicHolidayRepository.findByYearOrderByHolidayDateAsc(year)
                .stream().map(CalendarHolidayDto::from).toList();
    }

    // 기간 질의 (start~end 사이) — 내부적으로 연 캐시 합성 가능
    @Override
    public List<CalendarHolidayDto> getHolidaysBetween(LocalDate start, LocalDate end) {
        return publicHolidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end)
                .stream().map(CalendarHolidayDto::from).toList();
    }

    /**
     * 계산용: 기간 내 날짜 Set (중복/이름 제거)
     */
    public Set<LocalDate> dateSetBetween(LocalDate start, LocalDate end) {
        return getHolidaysBetween(start, end).stream()
                .map(CalendarHolidayDto::getDate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 계산용: 연도 걸침 최적화(연 단위 캐시 합성)
     */
    @Override
    public Set<LocalDate> dateSetSpanningYears(LocalDate start, LocalDate end) {
        int y1 = start.getYear(), y2 = end.getYear();
        var out = new LinkedHashSet<LocalDate>();
        for (int y = y1; y <= y2; y++) {
            out.addAll(getHolidaysOfYear(y).stream().map(CalendarHolidayDto::getDate).toList()); // 캐시 hit
        }
        out.removeIf(d -> d.isBefore(start) || d.isAfter(end));
        return out;
    }

    // (선택) 애플리케이션 기동 시 올해/내년 미리워밍
//    @EventListener(ApplicationReadyEvent.class)
//    public void warmUp() {
//        int y = LocalDate.now().getYear();
//        getHolidaysOfYear(y);
//        getHolidaysOfYear(y + 1);
//    }
}
