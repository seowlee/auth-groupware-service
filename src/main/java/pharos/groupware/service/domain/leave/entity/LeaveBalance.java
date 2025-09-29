package pharos.groupware.service.domain.leave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.domain.leave.dto.CarryOverLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.dto.CreateLeaveBalanceReqDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "leave_balances", schema = "groupware")
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type")
    private LeaveTypeEnum leaveType;

    @NotNull
    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @Column(name = "total_allocated", precision = 6, scale = 3)
    private BigDecimal totalAllocated;

    @Column(name = "used", precision = 6, scale = 3)
    private BigDecimal used;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public static LeaveBalance create(CreateLeaveBalanceReqDto reqDto) {
        LeaveBalance lb = new LeaveBalance();
        lb.userId = reqDto.getUserId();
        lb.leaveType = reqDto.getLeaveType();
        lb.yearNumber = reqDto.getYearNumber();
        lb.totalAllocated = reqDto.getTotalAllocated();
        lb.used = BigDecimal.ZERO;
        lb.createdAt = OffsetDateTime.now();
        lb.createdBy = "system";
        lb.updatedAt = OffsetDateTime.now();
        lb.updatedBy = "system";

        return lb;
    }

    public static LeaveBalance createBorrowed(CarryOverLeaveBalanceReqDto reqDto) {
        LeaveBalance lb = new LeaveBalance();
        lb.userId = reqDto.getUserId();
        lb.leaveType = reqDto.getLeaveType();
        lb.yearNumber = reqDto.getYearNumber();
        lb.totalAllocated = BigDecimal.ZERO;
        lb.used = reqDto.getUsed();
        lb.createdAt = OffsetDateTime.now();
        lb.createdBy = "system";
        lb.updatedAt = OffsetDateTime.now();
        lb.updatedBy = "system";

        return lb;
    }

    public void overwriteTotalAllocated(BigDecimal newTotal) {
        this.totalAllocated = LeaveUtils.nullToZero(newTotal);
        touch();
    }

    public void addUsed(BigDecimal newUsed) {
        this.used = this.used.add(LeaveUtils.nullToZero(newUsed));
        touch();
    }

    public void updateYearNumber(Integer newYearNumber) {
        this.yearNumber = newYearNumber;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = "system";
    }

    public void subtractUsed(BigDecimal days) {
        BigDecimal next = this.used.subtract(days);
        if (next.signum() < 0) next = BigDecimal.ZERO;
        this.used = LeaveUtils.scale(next);
        touch();
    }
}