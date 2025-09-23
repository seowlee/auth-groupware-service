package pharos.groupware.service.domain.holiday.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.domain.holiday.dto.CalendarHolidayDto;
import pharos.groupware.service.domain.holiday.dto.SyncReportResDto;
import pharos.groupware.service.domain.holiday.service.PublicHolidayService;
import pharos.groupware.service.domain.holiday.service.PublicHolidaySyncService;

import java.time.LocalDate;
import java.util.List;

import static pharos.groupware.service.common.util.DateUtils.parseToDate;

@Tag(name = "04. 공휴일 데이터 기능", description = "연차 일수 계산 위한 공휴일 공공데이터 수집 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holidays")
public class PublicHolidayController {
    private final PublicHolidaySyncService publicHolidaySyncService;
    private final PublicHolidayService publicHolidayService;


    @PostMapping("/sync-missing")
    public ResponseEntity<SyncReportResDto> syncMissing(@RequestParam int year) {
        SyncReportResDto res = publicHolidaySyncService.addMissingOnly(year);
        return ResponseEntity.ok(res);
    }

    // FullCalendar/일반용: 기간 기반
    @GetMapping
    public List<CalendarHolidayDto> getBetween(
            @RequestParam String start,   // ISO(yyyy-MM-dd or yyyy-MM-ddTHH:mm:ssZ)
            @RequestParam String end
    ) {
        LocalDate s = parseToDate(start, true);
        LocalDate e = parseToDate(end, false);
        return publicHolidayService.getHolidaysBetween(s, e);
    }

    // DatePicker 빠른 로딩용: 특정 연도
    @GetMapping("/year/{year}")
    public List<CalendarHolidayDto> getYear(@PathVariable int year) {
        return publicHolidayService.getHolidaysOfYear(year);
    }

}
