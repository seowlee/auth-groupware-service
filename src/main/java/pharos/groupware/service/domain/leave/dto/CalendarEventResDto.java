package pharos.groupware.service.domain.leave.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CalendarEventResDto {
    String id;       // leaveId
    String title;    // 캘린더 표시 텍스트 (뷰에서 마음대로 써도 됨)
    String start;    // ISO8601
    String end;      // ISO8601
    String status;   // extendedProps.status
    String type;     // extendedProps.type
    String userName; // extendedProps.userName
}