package pharos.groupware.service.domain.audit.dto;

import lombok.Data;

@Data
public class AuditLogSearchReq {
    private String keyword; // createdBy, ipAddress, action 에 like
    private String action;  // exact
    private String status;  // exact
}