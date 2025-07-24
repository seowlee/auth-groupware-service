package pharos.groupware.service.leave.service;

public interface LeaveBalanceService {
    void initializeLeaveBalancesForUser(Long userId, int yearNumber);

    void renewAnnualLeaveForToday();
}
