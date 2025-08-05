package pharos.groupware.service.leave.dto;

import lombok.Data;

@Data
public class LeaveBalanceResDto {
    private String typeCode;
    private String typeName;
    private int totalDays;
    private int usedDays;
    private int remainingDays;
}