package pharos.groupware.service.leave.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.team.domain.User;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(name = "leaves", schema = "groupware")
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveTypeEnum leaveType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatusEnum status;

    @Column(name = "reason", length = Integer.MAX_VALUE)
    private String reason;

    @Size(max = 512)
    @Column(name = "calendar_event_id", length = 512)
    private String calendarEventId;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Size(max = 50)
    @ColumnDefault("'system'")
    @Column(name = "updated_by", length = 50)
    private String updatedBy;


    public static Leave create(CreateLeaveReqDto dto, User user, String calendarEventId, String currentUsername) {
        Leave leave = new Leave();

        // String → LocalDateTime → OffsetDateTime(+09:00)
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        leave.startTime = dto.getStartTime().atZone(seoulZone).toOffsetDateTime();
        leave.endTime = dto.getEndTime().atZone(seoulZone).toOffsetDateTime();

//        leave.startTime = OffsetDateTime.parse(dto.getStartTime());
//        leave.endTime = OffsetDateTime.parse(dto.getEndTime());
        leave.leaveType = LeaveTypeEnum.valueOf(dto.getLeaveType());
        leave.status = LeaveStatusEnum.APPROVED;
        leave.reason = dto.getReason();
        leave.calendarEventId = calendarEventId;
        leave.userId = user.getId();
        leave.createdAt = OffsetDateTime.now();
        leave.createdBy = currentUsername;
        leave.updatedAt = OffsetDateTime.now();
        leave.updatedBy = currentUsername;
        return leave;
    }
}