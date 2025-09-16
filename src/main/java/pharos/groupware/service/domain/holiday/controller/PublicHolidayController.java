package pharos.groupware.service.domain.holiday.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.domain.holiday.dto.SyncReportResDto;
import pharos.groupware.service.domain.holiday.service.PublicHolidaySyncService;

@Tag(name = "04. 공휴일 데이터 기능", description = "연차 일수 계산 위한 공휴일 공공데이터 수집 관련 API")
@RestController
@RequiredArgsConstructor
public class PublicHolidayController {
    private final PublicHolidaySyncService service;

    @PostMapping("/sync-missing")
    public ResponseEntity<SyncReportResDto> syncMissing(@RequestParam int year) {
        SyncReportResDto res = service.addMissingOnly(year);
        return ResponseEntity.ok(res);
    }
}
