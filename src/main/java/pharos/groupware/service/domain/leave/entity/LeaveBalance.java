package pharos.groupware.service.domain.leave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
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
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveTypeEnum leaveType;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @ColumnDefault("0.000")
    @Column(name = "total_allocated", precision = 6, scale = 3)
    private BigDecimal totalAllocated;

    @ColumnDefault("0.000")
    @Column(name = "used", precision = 6, scale = 3)
    private BigDecimal used;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
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

    public void addUsed(BigDecimal newUsed, String actor) {
        this.used = this.used.add(LeaveUtils.nullToZero(newUsed));
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }


    private void touch() {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = "system";
    }

}