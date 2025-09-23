package pharos.groupware.service.domain.holiday.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.domain.holiday.dto.HolidayKey;
import pharos.groupware.service.domain.holiday.dto.SyncReportResDto;
import pharos.groupware.service.domain.holiday.entity.PublicHoliday;
import pharos.groupware.service.domain.holiday.entity.PublicHolidayRepository;
import pharos.groupware.service.infrastructure.publicapi.HolidayApiItem;
import pharos.groupware.service.infrastructure.publicapi.HolidayOpenApiClient;

import java.time.LocalDate;
import java.util.*;

import static pharos.groupware.service.common.util.CommonUtils.parseSeq;

@Service
@RequiredArgsConstructor
public class PublicHolidaySyncServiceImpl implements PublicHolidaySyncService {
    private final HolidayOpenApiClient client;
    private final PublicHolidayRepository repository;

    /**
     * 특정 연도 공휴일을 API에서 받아와 테이블에 반영.
     * 정책: 연도 단위로 전량 덮어쓰기(선삭제 후 삽입) 또는 UPSERT 중 선택.
     */
    @Override
    @Transactional
    public void syncYear(int year) {
        List<HolidayApiItem> items = client.fetchYear(year);
        if (items.isEmpty()) {
            System.out.println("items is empty");
            // 필요하면 로그만 남김
            return;
        }

        // 1) 기존 연도 데이터 삭제
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

        // 1) API -> (date, seq) 기준으로 정리
        Map<HolidayKey, HolidayApiItem> apiByKey = new LinkedHashMap<>();
        for (HolidayApiItem it : items) {
            LocalDate d = HolidayOpenApiClient.toDate(it.getLocdate());
            if (d == null) continue;
            short seq = parseSeq(it.getSeq());
            apiByKey.put(new HolidayKey(d, seq), it);
        }

        // 2) DB에 이미 있는 날짜 수집
        Set<HolidayKey> existingDates = repository.findAllKeysByYear(year);

        // 3) 신규 공휴일 탐지
        List<PublicHoliday> toInsert = new ArrayList<>();
        for (Map.Entry<HolidayKey, HolidayApiItem> e : apiByKey.entrySet()) {
            HolidayKey key = e.getKey();
            if (existingDates.contains(key)) continue; // 이미 있으면 skip

            HolidayApiItem it = e.getValue();
            toInsert.add(PublicHoliday.of(key.date(), it.getDateName(), key.seq(), year));
        }

        // 4) 저장
        if (!toInsert.isEmpty()) {
            repository.saveAll(toInsert);
        }

        return SyncReportResDto.ok(year, toInsert.size(), 0, items.size());
    }
}
