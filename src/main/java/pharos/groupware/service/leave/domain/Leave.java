package pharos.groupware.service.leave.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "leaves", schema = "groupware")
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Size(max = 30)
    @NotNull
    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType;

    @Size(max = 20)
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;

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

}