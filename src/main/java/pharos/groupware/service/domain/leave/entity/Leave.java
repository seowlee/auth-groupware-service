package pharos.groupware.service.domain.leave.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.util.DateUtils;
import pharos.groupware.service.domain.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveReqDto;
import pharos.groupware.service.domain.team.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

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
    private OffsetDateTime startDt;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endDt;

    @Column(name = "used_days", precision = 6, scale = 3, nullable = false)
    private BigDecimal usedDays;

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

    @ColumnDefault("CURRENT_DATE")
    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

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
        leave.startDt = DateUtils.toSeoulOffsetDateTime(dto.getStartDt());
        leave.endDt = DateUtils.toSeoulOffsetDateTime(dto.getEndDt());
        leave.usedDays = dto.getUsedDays();
        leave.leaveType = LeaveTypeEnum.valueOf(dto.getLeaveType());
        leave.status = LeaveStatusEnum.APPROVED;
        leave.reason = dto.getReason();
        leave.calendarEventId = calendarEventId;
        leave.userId = user.getId();
        leave.appliedAt = OffsetDateTime.now();
        leave.createdAt = OffsetDateTime.now();
        leave.createdBy = currentUsername;
        leave.updatedAt = OffsetDateTime.now();
        leave.updatedBy = currentUsername;
        return leave;
    }

    public void updateFrom(UpdateLeaveReqDto reqDto, String currentUsername) {
        this.leaveType = LeaveTypeEnum.valueOf(reqDto.getLeaveType());
        this.startDt = DateUtils.toSeoulOffsetDateTime(reqDto.getStartDt());
        this.endDt = DateUtils.toSeoulOffsetDateTime(reqDto.getEndDt());
        this.usedDays = reqDto.getUsedDays();
        this.reason = reqDto.getReason();
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = currentUsername;
    }

    public void cancel(String actor) {
        this.status = LeaveStatusEnum.CANCELED;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
        // 필요하면 캘린더 이벤트 아이디를 비우고 싶을 때:
        // this.calendarEventId = null;
    }

    public void validateUpdatable(LocalDateTime newStart, LocalDateTime newEnd, String newLeaveTypeStr) {
        // 1) 상태 체크
        if (status == LeaveStatusEnum.CANCELED || status == LeaveStatusEnum.REJECTED) {
            throw new IllegalStateException("취소/반려된 연차는 수정할 수 없습니다.");
        }
        if (this.endDt != null && this.endDt.isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("이미 종료된 연차는 수정할 수 없습니다.");
        }

        // 2) 파라미터 널/순서 검증
        if (newStart == null || newEnd == null) {
            throw new IllegalArgumentException("시작/종료 일시는 필수입니다.");
        }
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("종료 일시는 시작 일시보다 이후여야 합니다.");
        }

        // 3) 타입 파싱
        try {
            LeaveTypeEnum.valueOf(Objects.requireNonNull(newLeaveTypeStr, "연차 유형은 필수입니다."));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("연차 유형 값이 올바르지 않습니다: " + newLeaveTypeStr);
        }

        // 4) 근무 슬롯 제약 (09:00/13:00/17:00만 허용)
        //    - 시작은 09:00 또는 13:00
        //    - 종료는 13:00 또는 17:00
        //    - 같은 날이면 (09→13), (09→17), (13→17)만 허용
        Set<LocalTime> okStarts = Set.of(LocalTime.of(9, 0), LocalTime.of(13, 0));
        Set<LocalTime> okEnds = Set.of(LocalTime.of(13, 0), LocalTime.of(17, 0));

        LocalTime sTime = newStart.toLocalTime();
        LocalTime eTime = newEnd.toLocalTime();
        if (!okStarts.contains(sTime) || !okEnds.contains(eTime)) {
            throw new IllegalArgumentException("시작/종료 시간은 09:00/13:00/17:00 슬롯만 허용됩니다.");
        }
        if (newStart.toLocalDate().equals(newEnd.toLocalDate())) {
            boolean okSameDay =
                    (sTime.equals(LocalTime.of(9, 0)) && (eTime.equals(LocalTime.of(13, 0)) || eTime.equals(LocalTime.of(17, 0)))) ||
                            (sTime.equals(LocalTime.of(13, 0)) && eTime.equals(LocalTime.of(17, 0)));
            if (!okSameDay) {
                throw new IllegalArgumentException("같은 날짜에서는 (09→13), (09→17), (13→17) 조합만 허용됩니다.");
            }
        }

    }

    public void validateCancelable() {
        if (this.status == LeaveStatusEnum.CANCELED) {
            throw new IllegalStateException("이미 취소된 연차입니다.");
        }
        if (this.endDt != null && this.endDt.isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("이미 종료된 연차는 취소할 수 없습니다.");
        }
    }


}