package pharos.groupware.service.domain.leave.service;

import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;

import java.util.List;
import java.util.UUID;

public interface LeaveBalanceService {
    void initializeLeaveBalancesForUser(Long userId, int yearNumber);

    void renewAnnualLeaveForToday();

    List<LeaveBalanceResDto> getLeaveBalances(UUID uuid);
}
