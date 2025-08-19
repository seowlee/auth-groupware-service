package pharos.groupware.service.domain.policy.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pharos.groupware.service.domain.team.entity.User;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "work_policies", schema = "groupware")
public class WorkPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @ColumnDefault("'09:00:00'")
    @Column(name = "work_start", nullable = false)
    private LocalTime workStart;

    @NotNull
    @ColumnDefault("'18:00:00'")
    @Column(name = "work_end", nullable = false)
    private LocalTime workEnd;

    @NotNull
    @ColumnDefault("60")
    @Column(name = "lunch_break_minutes", nullable = false)
    private Integer lunchBreakMinutes;

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