package pharos.groupware.service.leave.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "leave_balances", schema = "groupware")
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 30)
    @NotNull
    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "year_number", nullable = false)
    private Integer yearNumber;

    @ColumnDefault("15.00")
    @Column(name = "total_allocated", precision = 5, scale = 2)
    private BigDecimal totalAllocated;

    @ColumnDefault("0.00")
    @Column(name = "used", precision = 5, scale = 2)
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

}