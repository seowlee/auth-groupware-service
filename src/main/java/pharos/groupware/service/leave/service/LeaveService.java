package pharos.groupware.service.leave.service;

import pharos.groupware.service.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.leave.dto.LeaveDetailResDto;

public interface LeaveService {
    void applyLeave(CreateLeaveReqDto reqDto);

    LeaveDetailResDto getLeaveDetail(Long id);
}
