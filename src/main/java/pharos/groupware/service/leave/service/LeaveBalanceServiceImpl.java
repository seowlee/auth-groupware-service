package pharos.groupware.service.leave.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.leave.domain.LeaveBalance;
import pharos.groupware.service.leave.domain.LeaveBalanceRepository;
import pharos.groupware.service.leave.dto.CreateLeaveBalanceReqDto;
import pharos.groupware.service.team.domain.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;

    @Override
    public void initializeLeaveBalancesForUser(Long userId, int yearNumber) {
        List<LeaveBalance> balances = Arrays.stream(LeaveTypeEnum.values())
                .filter(LeaveTypeEnum::isInitialGrant)
                .map(type -> {
                    int baseDays = type.getDefaultDays();
                    BigDecimal total = BigDecimal.valueOf(
                            type == LeaveTypeEnum.ANNUAL ? baseDays + (yearNumber - 1) : baseDays
                    );

                    return LeaveBalance.create(new CreateLeaveBalanceReqDto(
                            userId,
                            type,
                            yearNumber,
                            total
                    ));
                })
                .collect(Collectors.toList());

        leaveBalanceRepository.saveAll(balances);
    }

    @Override
    public void renewAnnualLeaveForToday() {
        LocalDate today = LocalDate.now();

//        List<User> users = userRepository.findAllWithJoinDateMonthDay(today);
//        for (User user : users) {
//            int newYearNumber = user.getYearNumber() + 1;
//            user.setYearNumber(newYearNumber); // 갱신
//
//            leaveBalanceService.initializeLeaveBalancesForUser(user.getId(), newYearNumber);
//        }

    }


}
