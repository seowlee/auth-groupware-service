package pharos.groupware.service.domain.leave.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.domain.leave.dto.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface LeaveService {
    Page<LeaveDetailResDto> getAllLeaves(LeaveSearchReqDto searchDto, Pageable pageable);

    LeaveDetailResDto getLeaveDetail(Long id);

    Long applyLeave(CreateLeaveReqDto reqDto, AppUser actor);

    Long updateLeave(Long id, UpdateLeaveReqDto reqDto, AppUser actor);

    void cancelLeave(Long id, AppUser actor);

    List<CalendarEventResDto> getCalendarEvents(
            Long teamId,
            LeaveTypeEnum type,
            LeaveStatusEnum status,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
