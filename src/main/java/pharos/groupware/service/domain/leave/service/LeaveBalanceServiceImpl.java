package pharos.groupware.service.domain.leave.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.excel.LeaveBalanceExcelExporter;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.domain.leave.dto.CarryOverLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.dto.CreateLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.entity.LeaveBalance;
import pharos.groupware.service.domain.leave.entity.LeaveBalanceRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceExcelExporter exporter;
    @Value("${export.leave-balance.dir:./exports}")
    private String exportDir;

    @Override
    @Transactional
    public void renewAnnualLeaveForToday() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        // 1) 지난달 1년차 월 적립(+1, 상한 11)
        grantForLastMonth();
        // 2) 이번 달이 입사월인 사용자 처리
        List<User> users = userRepository.findAllActiveByHiredDate(month, day);
        for (User user : users) {
            LocalDate joinedDt = user.getJoinedDate();
            if (!LeaveUtils.isAnniversaryDay(joinedDt)) continue;
            if (user.getYearNumber() == 1) continue;

            int newYearNumber = (int) java.time.temporal.ChronoUnit.YEARS.between(joinedDt, today) + 1;

            // 이미 승급돼 있다면 스킵 (중복 방지)
            if (user.getYearNumber() >= newYearNumber) continue;

            // 2-1) 초기부여 타입 upsert (ANNUAL은 정책, 1년차=0)
            initializeLeaveBalancesForUser(user.getId(), newYearNumber);
            // 2-2) 전년도 ADVANCE → 올해 BORROWED 이월
            carryOverAdvanceToBorrowed(user.getId(), newYearNumber);
            // 2-3) 사용자 입사연차 증가
            user.updateYearNumber(newYearNumber);

        }

    }

    @Override
    public void grantForLastMonth() {

        // 1년차 대상만 처리(= 월 적립 기간)
        List<User> users = userRepository.findAllActiveOfFirstYearNumber();
        for (User user : users) {
            if (user.getYearNumber() != 1) continue;

            LeaveBalance lb = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYearNumber(user.getId(), LeaveTypeEnum.ANNUAL, 1)
                    .orElseGet(() -> LeaveBalance.create(new CreateLeaveBalanceReqDto(
                            user.getId(), LeaveTypeEnum.ANNUAL, 1, BigDecimal.ZERO
                    )));

            // 상한 11
            BigDecimal cur = lb.getTotalAllocated() == null ? BigDecimal.ZERO : lb.getTotalAllocated();
            if (cur.intValue() < 11) {
                lb.overwriteTotalAllocated(cur.add(BigDecimal.ONE));
            }
        }
    }

    @Override
    public void initializeLeaveBalancesForUser(Long userId, int yearNumber) {
        List<LeaveTypeEnum> initialGrants =
                Arrays.stream(LeaveTypeEnum.values())
                        .filter(LeaveTypeEnum::isInitialGrant)
                        .toList();

        List<LeaveBalance> toInsert = new ArrayList<>(initialGrants.size());
        for (LeaveTypeEnum type : initialGrants) {
            BigDecimal total = (type == LeaveTypeEnum.ANNUAL)
                    ? BigDecimal.valueOf(LeaveUtils.annualGrantDays(yearNumber)) // 1년차=0
                    : BigDecimal.valueOf(type.getDefaultDays());

            toInsert.add(LeaveBalance.create(new CreateLeaveBalanceReqDto(
                    userId, type, yearNumber, total
            )));
        }
        leaveBalanceRepository.saveAll(toInsert);
//        List<LeaveBalance> balances = Arrays.stream(LeaveTypeEnum.values())
//                .filter(LeaveTypeEnum::isInitialGrant)
//                .map(type -> {
//                    int baseDays = type.getDefaultDays();
//                    BigDecimal total = BigDecimal.valueOf(
//                            type == LeaveTypeEnum.ANNUAL ? baseDays + (yearNumber - 1) : baseDays
//                    );
//
//                    return LeaveBalance.create(new CreateLeaveBalanceReqDto(
//                            userId,
//                            type,
//                            yearNumber,
//                            total
//                    ));
//                })
//                .collect(Collectors.toList());
//
//        leaveBalanceRepository.saveAll(balances);
    }

    public void carryOverAdvanceToBorrowed(Long userId, int newYearNumber) {
        int prevYearNumber = newYearNumber - 1;
        if (prevYearNumber < 1) return;

        LeaveBalance advanced = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYearNumber(userId, LeaveTypeEnum.ADVANCE, prevYearNumber).orElse(null);

        if (advanced == null) return;
        BigDecimal used = advanced.getUsed() == null ? BigDecimal.ZERO : advanced.getUsed();
        if (used.compareTo(BigDecimal.ZERO) <= 0) return;


        LeaveBalance borrowed = LeaveBalance.createBorrowed(new CarryOverLeaveBalanceReqDto(
                userId,
                LeaveTypeEnum.BORROWED,
                newYearNumber,
                advanced.getUsed()
        ));

        leaveBalanceRepository.save(borrowed);
    }

    @Override
    public List<LeaveBalanceResDto> getLeaveBalances(UUID uuid) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. userId(Long)로 연차 잔여 조회
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserId(user.getId());

        return balances.stream().map(balance -> {
            LeaveBalanceResDto dto = new LeaveBalanceResDto();
            dto.setTypeCode(balance.getLeaveType().name());
            dto.setTypeName(balance.getLeaveType().getDescription());
            dto.setRemainingDays(balance.getTotalAllocated().subtract(balance.getUsed()));
            dto.setUsedDays(balance.getUsed());
            dto.setTotalDays(balance.getTotalAllocated());
            return dto;
        }).collect(Collectors.toList());
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

            LeaveBalance lb = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYearNumber(user.getId(), type, year)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "LeaveBalance 없음: " + user.getId() + " / " + type + " / " + year));

            lb.overwriteTotalAllocated(item.getTotalAllocated().setScale(3, RoundingMode.HALF_UP));
        }
    }

    @Override
    @Transactional
    public void applyDelta(User user, LeaveTypeEnum type, int yearNumber, BigDecimal delta) {
        if (delta == null || delta.signum() == 0) return;
        if (delta.signum() > 0) {
            applyUsage(user, type, yearNumber, delta);
        } else {
            revertUsage(user, type, yearNumber, delta.abs());
        }
    }

    @Override
    @Transactional
    public void applyUsage(User user, LeaveTypeEnum type, int yearNumber, BigDecimal usedDays) {
        if (usedDays.signum() <= 0) return;

        // 1) 타입별 정책 분기
        if (type == LeaveTypeEnum.ANNUAL) {
            applyAnnual(user.getId(), yearNumber, usedDays);
            return;
        }

        // initialGrant=false 타입: 없으면 생성해서 used만 채움, 있으면 used +=
        LeaveBalance lb = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYearNumber(user.getId(), type, yearNumber)
                .orElseGet(() -> {
                    // 신규 생성: totalAllocated는 defaultDays(한도형), 아니면 0
                    BigDecimal alloc = BigDecimal.valueOf(type.getDefaultDays());
                    LeaveBalance created = LeaveBalance.create(new CreateLeaveBalanceReqDto(
                            user.getId(), type, yearNumber, alloc
                    ));
                    return created;
                });

        BigDecimal currentUsed = LeaveUtils.nullToZero(lb.getUsed());
        BigDecimal currentAlloc = LeaveUtils.nullToZero(lb.getTotalAllocated());
        BigDecimal afterUsed = currentUsed.add(usedDays);

        // 한도 존재(type.getDefaultDays()>0) & 초과 처리
        if (type == LeaveTypeEnum.BIRTHDAY) {
            // 생일연차는 초과 불가
            if (afterUsed.compareTo(currentAlloc) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "생일연차 잔액 부족");
            }
            lb.addUsed(usedDays);
        } else if (type.getDefaultDays() > 0) { //TODO: 비교값 수정 lb.getTotalAllocated()?
            // 한도 초과분은 ADVANCE에 기록
            BigDecimal shortage = afterUsed.subtract(currentAlloc);
            if (shortage.signum() > 0) {
                // 우선 가능한 만큼만 사용
                BigDecimal usable = usedDays.subtract(shortage);
                if (usable.signum() > 0) lb.addUsed(usable);
                // 남는 부족분은 ADVANCE.used 증가
                applyAdvance(user.getId(), yearNumber, shortage);
            } else {
                // 한도 내
                lb.addUsed(usedDays);
            }
        } else {
            // 한도 없는(non-cap) 약정/공가/보상 등: 그냥 누적
            lb.addUsed(usedDays);
        }

        // 신규면 저장
        if (lb.getId() == null) leaveBalanceRepository.save(lb);
    }

    @Override
    public void revertUsage(User user, LeaveTypeEnum type, int yearNumber, BigDecimal usedDays) {
        if (usedDays.signum() <= 0) return;
        final String actor = user.getUsername() == null ? "system" : user.getUsername();

        LeaveBalance lb = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYearNumber(user.getId(), type, yearNumber)
                .orElseThrow(() -> new EntityNotFoundException("해당 LeaveBalance가 없습니다."));
        lb.subtractUsed(usedDays);
    }


    @Override
    @Transactional(readOnly = true)
    public Path exportLatestBalances(LeaveTypeEnum filter) {
        // 유저별 최신 yearNumber의 모든 타입
        List<LeaveBalance> rows = leaveBalanceRepository.findLatestYearBalances(filter);

        // 또는: 유저×타입별 최신 1행씩
        // List<LeaveBalance> rows = leaveBalanceRepository.findLatestPerUserAndType();

        Map<Long, String> userNameMap = userRepository.findAllById(
                rows.stream().map(LeaveBalance::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getUsername));

        return exporter.export(rows, userNameMap, Paths.get(exportDir));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reallocateAnnualOnJoinedDateChange(Long userId, Integer beforeYearNo, Integer afterYearNo) {
        List<LeaveBalance> balances = leaveBalanceRepository
                .findByUserIdAndYearNumber(userId, beforeYearNo);

        if (balances.isEmpty()) {
            throw new EntityNotFoundException("해당 연도의 LeaveBalance가 없습니다. userId="
                    + userId + ", year=" + beforeYearNo);
        }

        for (LeaveBalance lb : balances) {
            // 모든 LeaveBalance의 연도 업데이트
            lb.updateYearNumber(afterYearNo);

            // ANNUAL만 총 연차 일수 재계산
            if (lb.getLeaveType() == LeaveTypeEnum.ANNUAL) {
                BigDecimal newTotal = BigDecimal.valueOf(LeaveUtils.annualGrantDays(afterYearNo));
                lb.overwriteTotalAllocated(newTotal.setScale(3, RoundingMode.HALF_UP));
            }
        }
    }

    /**
     * ANNUAL: 잔액 부족분은 ADVANCE.used로 기록
     */
    private void applyAnnual(Long userId, int yearNumber, BigDecimal usedDays) {
        // 1) 연차 버킷 잠금 조회(없으면 예외 or 0할당 생성? 정책에 따라)
        LeaveBalance annual = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYearNumber(userId, LeaveTypeEnum.ANNUAL, yearNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "연차 보유 정보가 없습니다."));

        BigDecimal alloc = LeaveUtils.nullToZero(annual.getTotalAllocated());
        BigDecimal used = LeaveUtils.nullToZero(annual.getUsed());
        BigDecimal remain = alloc.subtract(used);

        if (remain.compareTo(usedDays) >= 0) {
            // 전부 ANNUAL에서 차감
            annual.addUsed(usedDays);
            return;
        }

        // 일부만 ANNUAL, 나머지는 ADVANCE
        if (remain.signum() > 0) annual.addUsed(remain);
        BigDecimal shortage = usedDays.subtract(remain);
        applyAdvance(userId, yearNumber, shortage);
    }

    /**
     * 부족분을 빌려쓴연차(ADVANCE.used)에 누적 (없으면 생성 total=0)
     */
    private void applyAdvance(Long userId, int yearNumber, BigDecimal delta) {
        if (delta == null || delta.signum() <= 0) return;

        LeaveBalance adv = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYearNumber(userId, LeaveTypeEnum.ADVANCE, yearNumber)
                .orElseGet(() -> LeaveBalance.create(new CreateLeaveBalanceReqDto(
                        userId, LeaveTypeEnum.ADVANCE, yearNumber, BigDecimal.ZERO
                )));

        adv.addUsed(delta);
        if (adv.getId() == null) leaveBalanceRepository.save(adv);
    }


}
