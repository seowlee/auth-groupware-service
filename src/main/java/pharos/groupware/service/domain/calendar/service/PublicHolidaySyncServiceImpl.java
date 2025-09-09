package pharos.groupware.service.domain.calendar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.domain.calendar.entity.PublicHoliday;
import pharos.groupware.service.domain.calendar.entity.PublicHolidayRepository;
import pharos.groupware.service.infrastructure.publicapi.HolidayApiItem;
import pharos.groupware.service.infrastructure.publicapi.HolidayOpenApiClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    @Transactional
    public void syncNextYear() {
        int year = LocalDate.now().getYear();
        syncYear(year + 1);
    }
}
