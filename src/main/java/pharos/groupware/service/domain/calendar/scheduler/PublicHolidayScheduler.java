package pharos.groupware.service.domain.calendar.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pharos.groupware.service.domain.calendar.service.PublicHolidaySyncService;

@Component
@RequiredArgsConstructor
public class PublicHolidayScheduler {
    private final PublicHolidaySyncService service;

    // 매년 1월 1일 03:10 KST
    @Scheduled(cron = "0 1 0 1 9 *", zone = "Asia/Seoul")
    public void syncEveryYear() {
        service.syncNextYear();
    }
}