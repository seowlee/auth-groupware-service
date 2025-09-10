package pharos.groupware.service.domain.calendar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.domain.calendar.dto.SyncReportResDto;
import pharos.groupware.service.domain.calendar.entity.PublicHoliday;
import pharos.groupware.service.domain.calendar.entity.PublicHolidayRepository;
import pharos.groupware.service.infrastructure.publicapi.HolidayApiItem;
import pharos.groupware.service.infrastructure.publicapi.HolidayOpenApiClient;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PublicHolidaySyncServiceImpl implements PublicHolidaySyncService {
    private final HolidayOpenApiClient client;
    private final PublicHolidayRepository repository;

    /**
     * 특정 연도 공휴일을 API에서 받아와 테이블에 반영.
     * 정책: 연도 단위로 전량 덮어쓰기(선삭제 후 삽입) 또는 UPSERT 중 선택.
     */
    @Transactional
    public void syncYear(int year) {
        List<HolidayApiItem> items = client.fetchYear(year);
        if (items.isEmpty()) {
            System.out.println("items is empty");
            // 필요하면 로그만 남김
            return;
        }

        // 1) 기존 연도 데이터 삭제(가장 간단/명확)
        repository.deleteAllByYear(year);

        // 2) INSERT
        List<PublicHoliday> rows = new ArrayList<>(items.size());
        for (HolidayApiItem it : items) {
            LocalDate date = HolidayOpenApiClient.toDate(it.getLocdate());
            rows.add(PublicHoliday.of(date, it.getDateName(), Short.parseShort(it.getSeq()), year));
        }
        System.out.println("rows size: " + rows.size());
        repository.saveAll(rows);
    }

    /**
     * 내년 공휴일 갱신
     */
    @Override
    @Transactional
    public void syncNextYear() {
        int year = LocalDate.now().getYear();
        syncYear(year + 1);
    }

    /**
     * API 결과를 기준으로 "없는 날짜만" INSERT (이름/seq 변경은 무시)
     */
    @Override
    @Transactional
    public SyncReportResDto addMissingOnly(int year) {
        List<HolidayApiItem> items = client.fetchYear(year);
        if (items == null || items.isEmpty()) {
            return SyncReportResDto.fail(year, "API returned empty list");
        }

        // 1) API -> (날짜 -> 아이템) 정리
        Map<LocalDate, HolidayApiItem> apiMap = new LinkedHashMap<>();
        for (HolidayApiItem it : items) {
            LocalDate d = HolidayOpenApiClient.toDate(it.getLocdate());
            if (d != null) apiMap.put(d, it); // 중복 날짜 있으면 마지막 값으로 덮음
        }

        // 2) DB에 이미 있는 날짜 수집
        Set<LocalDate> existingDates = repository.findAllDatesByYear(year);

        // 3) 없는 날짜만 rows 생성
        List<PublicHoliday> toInsert = new ArrayList<>();
        for (Map.Entry<LocalDate, HolidayApiItem> e : apiMap.entrySet()) {
            if (existingDates.contains(e.getKey())) continue; // 이미 있으면 skip

            HolidayApiItem it = e.getValue();
            short seq = Short.parseShort(it.getSeq());
            toInsert.add(PublicHoliday.of(e.getKey(), it.getDateName(), seq, year));
        }

        // 4) 저장
        if (!toInsert.isEmpty()) {
            repository.saveAll(toInsert);
        }

        return SyncReportResDto.ok(year, toInsert.size(), 0, apiMap.size());
    }
}
