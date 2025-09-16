package pharos.groupware.service.domain.leave.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveBalanceScheduler {
    private final LeaveBalanceService leaveBalanceService;

    @Scheduled(cron = "0 5 2 * * ?", zone = "Asia/Seoul")
    public void autoRenew() {
        leaveBalanceService.renewAnnualLeaveForToday();
    }

    /**
     * 연차 보유량 xlsx export 배치
     */
    @Scheduled(cron = "0 20 3 1 * ?")
    public void exportMonthly() {
        Path file = leaveBalanceService.exportLatestBalances(null); // 전체 타입
        log.info("LeaveBalance xlsx exported: {}", file);
    }

}
