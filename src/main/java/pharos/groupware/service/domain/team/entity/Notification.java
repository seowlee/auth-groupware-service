package pharos.groupware.service.domain.team.entity;

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
@Table(name = "notifications", schema = "groupware")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @Column(name = "type", length = 50)
    private String type;

    @NotNull
    @Column(name = "message", nullable = false, length = Integer.MAX_VALUE)
    private String message;

    @ColumnDefault("false")
    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

}