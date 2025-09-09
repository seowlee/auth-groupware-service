package pharos.groupware.service.domain.leave.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveBalanceResDto {
    private String typeCode;
    private String typeName;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
}