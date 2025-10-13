package pharos.groupware.service.domain.leave.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.domain.leave.entity.LeaveRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LeavePolicyServiceImpl implements LeavePolicyService {

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceService leaveBalanceService;

    public LeavePolicyServiceImpl(LeaveRepository leaveRepository,
                                  LeaveBalanceService leaveBalanceService) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceService = leaveBalanceService;
    }

    // ─────────────────────
    // 겹침(Overlap) 정책
    // ─────────────────────

    @Override
    @Transactional(readOnly = true)
    public void assertNoOverlapOrThrow(UUID userUuid, LocalDateTime start, LocalDateTime end) {
        OffsetDateTime startDt = DateUtils.toSeoulOffsetDateTime(start);
        OffsetDateTime endDt = DateUtils.toSeoulOffsetDateTime(end);
        List<LeaveStatusEnum> blocking = List.of(LeaveStatusEnum.APPROVED, LeaveStatusEnum.PENDING);

        boolean exists = leaveRepository.existsOverlapForUser(userUuid, startDt, endDt, blocking);
        if (exists) {
            throw new IllegalArgumentException("이미 신청된 연차와 기간이 겹칩니다. 기간을 조정한 뒤 다시 신청해 주세요.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertNoOverlapOrThrow(UUID userUuid, Long excludeLeaveId, LocalDateTime start, LocalDateTime end) {
        OffsetDateTime startDt = DateUtils.toSeoulOffsetDateTime(start);
        OffsetDateTime endDt = DateUtils.toSeoulOffsetDateTime(end);
        List<LeaveStatusEnum> blocking = List.of(LeaveStatusEnum.APPROVED, LeaveStatusEnum.PENDING);
        boolean exists = leaveRepository.existsOverlapForUserExcludingId(userUuid, excludeLeaveId, startDt, endDt, blocking);
        if (exists) {
            throw new IllegalArgumentException("이미 신청된 연차와 기간이 겹칩니다. 기간을 조정한 뒤 다시 시도해 주세요.");
        }
    }
}
