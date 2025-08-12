package pharos.groupware.service.domain.audit.entity;

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
@Table(name = "audit_logs", schema = "groupware")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 255)
    @NotNull
    @Column(name = "action", nullable = false)
    private String action;

    @Size(max = 50)
    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "detail", length = Integer.MAX_VALUE)
    private String detail;

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