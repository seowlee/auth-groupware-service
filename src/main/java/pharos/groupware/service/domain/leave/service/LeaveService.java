package pharos.groupware.service.domain.leave.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.domain.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveDetailResDto;
import pharos.groupware.service.domain.leave.dto.LeaveSearchReqDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveReqDto;

public interface LeaveService {
    Long applyLeave(CreateLeaveReqDto reqDto);

    LeaveDetailResDto getLeaveDetail(Long id);

    Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable);

    Long updateLeave(Long id, UpdateLeaveReqDto reqDto);

    void cancelLeave(Long id);
}
