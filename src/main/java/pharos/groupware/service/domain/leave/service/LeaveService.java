package pharos.groupware.service.domain.leave.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.domain.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveDetailResDto;
import pharos.groupware.service.domain.leave.dto.LeaveSearchReqDto;

public interface LeaveService {
    void applyLeave(CreateLeaveReqDto reqDto);

    LeaveDetailResDto getLeaveDetail(Long id);

    Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable);

    void updateLeave(Long id, CreateLeaveReqDto reqDto);

    void cancelLeave(Long id);
}
