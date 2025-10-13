package pharos.groupware.service.domain.leave.service;

import jakarta.validation.Valid;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveBalanceReqDto;
import pharos.groupware.service.domain.team.entity.User;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface LeaveBalanceService {
    void initializeLeaveBalancesForUser(Long userId, int yearNumber);

    void renewAnnualLeaveForToday();

    void grantForLastMonth();

    List<LeaveBalanceResDto> getLeaveBalances(UUID uuid);

    void update(UUID uuid, @Valid List<UpdateLeaveBalanceReqDto> reqDto);


    /**
     * 수정 시: (새사용량 - 이전사용량) 만큼만 증감 적용
     */
    void applyDelta(User user, LeaveTypeEnum type, int yearNumber, BigDecimal delta);

    /**
     * 생성 시: usedDays 전체 반영 (기존과 동일)
     */
    void applyUsage(User user, LeaveTypeEnum type, int yearNumber, BigDecimal usedDays);

    /**
     * 취소/되돌리기: usedDays 전체 복구 (기존과 동일)
     */
    void revertUsage(User user, LeaveTypeEnum type, int yearNumber, BigDecimal usedDays);

    Path exportLatestBalances(LeaveTypeEnum filter);

    void reallocateAnnualOnJoinedDateChange(Long userId, Integer beforeYearNo, Integer afterYearNo);
}
