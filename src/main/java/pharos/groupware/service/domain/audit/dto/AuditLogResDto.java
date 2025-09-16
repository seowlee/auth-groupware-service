package pharos.groupware.service.domain.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pharos.groupware.service.domain.audit.entity.AuditLog;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResDto {
    private Long id;
    private Long userId;
    private String createdBy;
    private String ipAddress;
    private String action;
    private String status;
    private OffsetDateTime createdAt;

    public static AuditLogResDto from(AuditLog a) {
        AuditLogResDto d = new AuditLogResDto();
        d.setId(a.getId());
        d.setUserId(a.getUserId());
        d.setCreatedBy(a.getCreatedBy());
        d.setIpAddress(a.getIpAddress());
        d.setAction(a.getAction());
        d.setStatus(a.getStatus());
        d.setCreatedAt(a.getCreatedAt());
        return d;
    }
}