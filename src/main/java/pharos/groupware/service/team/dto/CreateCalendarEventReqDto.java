package pharos.groupware.service.team.dto;

import lombok.Data;

@Data
public class CreateCalendarEventReqDto {
    private String graphUserId; // 예: userPrincipalName 또는 UUID
    private String subject;
    private String bodyContent;
    private String startDateTime; // ISO 8601: "2025-07-21T10:00:00"
    private String endDateTime;   // ISO 8601: "2025-07-21T11:00:00"
    private String timezone;      // 예: "Asia/Seoul"
}
