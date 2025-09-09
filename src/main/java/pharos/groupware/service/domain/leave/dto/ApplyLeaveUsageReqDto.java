package pharos.groupware.service.domain.leave.dto;

import lombok.Data;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

import java.math.BigDecimal;

@Data
public class ApplyLeaveUsageReqDto {
    private Long userId;
    private LeaveTypeEnum leaveType;
    private Integer yearNumber;
    private BigDecimal usedDays;

}
