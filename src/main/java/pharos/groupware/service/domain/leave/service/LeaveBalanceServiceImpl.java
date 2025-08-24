package pharos.groupware.service.domain.leave.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.domain.leave.dto.CreateLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.entity.LeaveBalance;
import pharos.groupware.service.domain.leave.entity.LeaveBalanceRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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

    @Override
    public List<LeaveBalanceResDto> getLeaveBalances(UUID uuid) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. userId(Long)로 연차 잔여 조회
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserId(user.getId());

//        return balances.stream().map(balance -> {
//            LeaveBalanceResDto dto = new LeaveBalanceResDto();
//            dto.setTypeCode(balance.getLeaveType().name());
//            dto.setTypeName(balance.getType().getDisplayName());
//            dto.setRemainingDays(balance.getRemainingDays());
//            dto.setUsedDays(balance.getUsedDays());
//            dto.setTotalDays(balance.getTotalDays());
//            return dto;
//        }).collect(Collectors.toList());
        LeaveBalanceResDto dto = new LeaveBalanceResDto();
        return Collections.singletonList(dto);
    }

    @Override
    @Transactional
    public void update(UUID uuid, @Valid List<UpdateLeaveBalanceReqDto> reqDto) {
        if (reqDto == null || reqDto.isEmpty()) return;
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        for (UpdateLeaveBalanceReqDto item : reqDto) {
            if (item.getLeaveType() == null || item.getTotalAllocated() == null) continue;

            LeaveTypeEnum type;
            try {
                type = LeaveTypeEnum.valueOf(item.getLeaveType());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leaveType이 올바르지 않습니다: " + item.getLeaveType());
            }

            Integer year = item.getYearNumber() != null ? item.getYearNumber() : user.getYearNumber();
            if (year == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "yearNumber가 필요합니다.");
            }

            LeaveBalance entity = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYearNumber(user.getId(), type, year)
                    .orElseGet(() -> {
                        LeaveBalance nb = new LeaveBalance();
                        nb.setUserId(user.getId());
                        nb.setLeaveType(type);
                        nb.setYearNumber(year);
                        nb.setUsed(BigDecimal.ZERO); // 시스템이 관리
                        return nb;
                    });

            entity.setTotalAllocated(item.getTotalAllocated().setScale(3, RoundingMode.HALF_UP));
            leaveBalanceRepository.save(entity);
        }
    }


}
