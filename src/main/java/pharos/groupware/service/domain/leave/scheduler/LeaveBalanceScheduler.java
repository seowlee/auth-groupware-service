package pharos.groupware.service.domain.leave.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;

@Component
@RequiredArgsConstructor
public class LeaveBalanceScheduler {
    private final LeaveBalanceService leaveBalanceService;

    @Scheduled(cron = "0 10 3 * * ?", zone = "Asia/Seoul")
    public void autoRenew() {
        leaveBalanceService.renewAnnualLeaveForToday();
    }
}
