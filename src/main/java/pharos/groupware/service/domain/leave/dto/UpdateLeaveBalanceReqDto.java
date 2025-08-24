package pharos.groupware.service.domain.leave.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLeaveBalanceReqDto {
    private String leaveType;            // "ANNUAL", "SICK", ...
    private Integer yearNumber;          // null이면 user.getYearNumber() 사용
    @PositiveOrZero
    private BigDecimal totalAllocated;   // NUMERIC(6,3)
}
