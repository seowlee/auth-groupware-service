package pharos.groupware.service.leave.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.leave.service.LeaveBalanceService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/leave")
public class LeaveBatchController {
    private final LeaveBalanceService leaveBalanceService;

//    @Scheduled(cron = "0 0 3 * * ?") // 매일 03:00 실행
//    public void autoRenew() {
//        leaveBalanceService.renewAnnualLeaveForToday();
//    }
}
