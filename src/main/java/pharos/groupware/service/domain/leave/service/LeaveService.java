package pharos.groupware.service.domain.leave.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.domain.leave.dto.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface LeaveService {
    Long applyLeave(CreateLeaveReqDto reqDto);

    LeaveDetailResDto getLeaveDetail(Long id);

    Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable);

    Long updateLeave(Long id, UpdateLeaveReqDto reqDto);

    void cancelLeave(Long id);

    List<CalendarEventResDto> getCalendarEvents(
            Long teamId,
            LeaveTypeEnum type,
            LeaveStatusEnum status,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
