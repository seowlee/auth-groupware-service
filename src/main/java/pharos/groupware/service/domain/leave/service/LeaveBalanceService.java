package pharos.groupware.service.domain.leave.service;

import jakarta.validation.Valid;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveBalanceReqDto;

import java.util.List;
import java.util.UUID;

public interface LeaveBalanceService {
    void initializeLeaveBalancesForUser(Long userId, int yearNumber);

    void renewAnnualLeaveForToday();

    List<LeaveBalanceResDto> getLeaveBalances(UUID uuid);

    void update(UUID uuid, @Valid List<UpdateLeaveBalanceReqDto> reqDto);
}
