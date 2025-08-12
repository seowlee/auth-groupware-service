package pharos.groupware.service.domain.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreateLeaveBalanceReqDto {
    private Long userId;
    private LeaveTypeEnum leaveType;
    private Integer yearNumber;
    private BigDecimal totalAllocated;

}
