package pharos.groupware.service.domain.audit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import pharos.groupware.service.domain.audit.dto.CreateAuditLogReqDto;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "audit_logs", schema = "groupware")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

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

    public static AuditLog create(CreateAuditLogReqDto dto) {
        AuditLog al = new AuditLog();
        al.userId = dto.getUserId();
        al.ipAddress = dto.getIpAddress();
        al.action = dto.getAction();
        al.status = dto.getStatus();
        // detail에 summary를 포함시키고 싶으면 JSON에 같이 넣거나, 필드를 따로 둘 수 있음(아래 2번 참고)
        al.detail = dto.getDetailJson();
        al.createdAt = OffsetDateTime.now();
        al.createdBy = (dto.getActor() == null || dto.getActor().isBlank()) ? "system" : dto.getActor();
        al.updatedAt = al.createdAt;
        al.updatedBy = al.createdBy;
        return al;
    }
}