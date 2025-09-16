package pharos.groupware.service.domain.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.domain.audit.dto.AuditLogResDto;
import pharos.groupware.service.domain.audit.dto.AuditLogSearchReq;

import java.util.Map;

public interface AuditLogService {
    /**
     * 범용 RAW 저장: detailObject를 그대로 JSON 직렬화하여 저장(REQUIRES_NEW).
     */
    Long saveLog(Long actorUserId, String actorUsername,
                 AuditActionEnum action, AuditStatusEnum status, Object detailObject);

    void saveSystemLog(AuditActionEnum action, AuditStatusEnum status, Map<String, Object> detail);

    void saveJobLog(String jobName, AuditActionEnum action, AuditStatusEnum status, Map<String, Object> detail);

    Page<AuditLogResDto> getLogs(AuditLogSearchReq req, Pageable pageable);
}
