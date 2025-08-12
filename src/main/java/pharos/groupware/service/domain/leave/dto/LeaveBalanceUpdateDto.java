package pharos.groupware.service.domain.leave.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveBalanceUpdateDto {
    private String typeCode;      // or typeName
    private BigDecimal remainingDays;
}
