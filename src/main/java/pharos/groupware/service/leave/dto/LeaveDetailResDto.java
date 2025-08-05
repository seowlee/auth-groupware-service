package pharos.groupware.service.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import pharos.groupware.service.leave.domain.Leave;
import pharos.groupware.service.team.domain.User;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@Schema(description = "연차 상세 응답 DTO")
public class LeaveDetailResDto {

    @Schema(description = "연차 ID", example = "12")
    private Long id;

    @Schema(description = "사용자 UUID", example = "c6f2aeb2-bfa1-4ab7-9b32-7cd187c9e9af")
    private String userUuid;

    private String userName;
    private String userEmail;

    @Schema(description = "연차 시작 시각 (ISO 8601)", example = "2025-08-21T09:00:00+09:00")
    private String startTime;

    @Schema(description = "연차 종료 시각 (ISO 8601)", example = "2025-08-21T18:00:00+09:00")
    private String endTime;

    @Schema(description = "연차 유형", example = "ANNUAL")
    private String leaveType;

    @Schema(description = "연차 상태", example = "PENDING")
    private String status;

    @Schema(description = "사유", example = "개인 사정")
    private String reason;

    @Schema(description = "Calendar 이벤트 ID")
    private String calendarEventId;


    public static LeaveDetailResDto fromEntity(Leave leave, User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

        return LeaveDetailResDto.builder()
                .id(leave.getId())
                .userUuid(user.getUserUuid().toString())
                .userName(user.getUsername())
                .userEmail(user.getEmail())
                .startTime(formatter.format(leave.getStartTime()))
                .endTime(formatter.format(leave.getEndTime()))
                .leaveType(leave.getLeaveType().name())
                .status(leave.getStatus().name())
                .reason(leave.getReason())
                .calendarEventId(leave.getCalendarEventId())
                .build();
    }
}
