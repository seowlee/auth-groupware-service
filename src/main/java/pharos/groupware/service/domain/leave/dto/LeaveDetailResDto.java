package pharos.groupware.service.domain.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.domain.leave.entity.Leave;
import pharos.groupware.service.domain.team.entity.User;

import java.math.BigDecimal;

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
    private String startDt;

    @Schema(description = "연차 종료 시각 (ISO 8601)", example = "2025-08-21T18:00:00+09:00")
    private String endDt;

    @Schema(description = "연차 신청 사용 일수", example = "2.500")
    private BigDecimal usedDays;

    @Schema(description = "연차 유형", example = "ANNUAL")
    private String leaveType;

    @Schema(description = "연차 상태", example = "PENDING")
    private String status;

    @Schema(description = "사유", example = "개인 사정")
    private String reason;

    @Schema(description = "Calendar 이벤트 ID")
    private String calendarEventId;

    @Schema(description = "신청 일자")
    private String appliedAt;

    // 권한/표시 제어용 플래그
    private boolean canEdit;        // 편집 버튼/입력 허용
    private boolean canCancel;      // 취소 버튼 허용
    private boolean canViewReason;  // 사유 필드(실제 값) 내려줄지 여부

    // leave detail
    public static LeaveDetailResDto fromEntity(Leave leave, User user) {

        return LeaveDetailResDto.builder()
                .id(leave.getId())
                .userUuid(user.getUserUuid().toString())
                .userName(user.getUsername())
                .userEmail(user.getEmail())
                .startDt(DateUtils.formatKst(leave.getStartDt()))
                .endDt(DateUtils.formatKst(leave.getEndDt()))
                .usedDays(leave.getUsedDays())
                .leaveType(leave.getLeaveType().name())
                .status(leave.getStatus().name())
                .reason(leave.getReason())
                .calendarEventId(leave.getCalendarEventId())
                .build();
    }

    // leave list
    public static LeaveDetailResDto fromEntity(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave is null");
        }

        User user = leave.getUser(); // fetch join 되어 있어야 함
        if (user == null) {
            throw new IllegalStateException("Leave.user is null – fetch join 안 되었을 가능성 있음");
        }


        return LeaveDetailResDto.builder()
                .id(leave.getId())
                .userUuid(user.getUserUuid().toString())
                .userName(user.getUsername())
                .userEmail(user.getEmail())
                .startDt(DateUtils.formatKst(leave.getStartDt()))
                .endDt(DateUtils.formatKst(leave.getEndDt()))
                .usedDays(leave.getUsedDays())
                .leaveType(leave.getLeaveType().name())
                .status(leave.getStatus().name())
                .reason(leave.getReason())
                .calendarEventId(leave.getCalendarEventId())
                .appliedAt(DateUtils.formatKst(leave.getAppliedAt()))
                .build();
    }

    // 권한/상태를 반영해서 만드는 팩토리
    public static LeaveDetailResDto fromEntityWithPerms(Leave leave, User owner,
                                                        boolean canEdit, boolean canCancel, boolean canViewReason) {
        return LeaveDetailResDto.builder()
                .id(leave.getId())
                .userUuid(owner.getUserUuid().toString())
                .userName(owner.getUsername())
                .userEmail(owner.getEmail())
                .startDt(DateUtils.formatKst(leave.getStartDt()))
                .endDt(DateUtils.formatKst(leave.getEndDt()))
                .usedDays(leave.getUsedDays())
                .leaveType(leave.getLeaveType().name())
                .status(leave.getStatus().name())
                .reason(canViewReason ? leave.getReason() : null) // 🔐 마스킹
                .calendarEventId(leave.getCalendarEventId())
                .canEdit(canEdit)
                .canCancel(canCancel)
                .canViewReason(canViewReason)
                .build();
    }
}
