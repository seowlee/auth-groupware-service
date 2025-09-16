package pharos.groupware.service.domain.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.common.util.AuditLogUtils;
import pharos.groupware.service.domain.audit.dto.AuditLogResDto;
import pharos.groupware.service.domain.audit.dto.AuditLogSearchReq;
import pharos.groupware.service.domain.audit.dto.CreateAuditLogReqDto;
import pharos.groupware.service.domain.audit.entity.AuditLog;
import pharos.groupware.service.domain.audit.entity.AuditLogRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;


    /**
     * 범용 RAW 저장.
     * - 트랜잭션 분리(REQUIRES_NEW): 비즈니스 트랜잭션 실패와 무관하게 감사 로그는 최대한 남긴다.
     * - detailObject: 실행 당시 DTO, Map 등 무엇이든 OK (Jackson 직렬화).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveLog(Long actorUserId, String actorUsername, AuditActionEnum action, AuditStatusEnum status, Object detailObject) {
        String json = AuditLogUtils.toJson(detailObject);

        var dto = CreateAuditLogReqDto.builder()
                .userId(actorUserId)
                .ipAddress(AuditLogUtils.currentIp())
                .actor(actorUsername)
                .action(action.name())
                .status(status.name())
                .detailJson(json)
                .build();

        return auditLogRepository.save(AuditLog.create(dto)).getId();
    }

    @Override
    public void saveSystemLog(AuditActionEnum action, AuditStatusEnum status, Map<String, Object> detail) {
        // actorId=null, createdBy="system"
        saveLog(null, "system", action, status, detail);
    }

    @Override
    public void saveJobLog(String jobName, AuditActionEnum action, AuditStatusEnum status, Map<String, Object> detail) {
        // actorId=null, createdBy="job:..." 로 남기면 추적이 쉬움
        saveLog(null, "job:" + jobName, action, status, detail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResDto> getLogs(AuditLogSearchReq req, Pageable pageable) {
        // 정렬 기본값: createdAt desc, id asc
        if (pageable.getSort().isUnsorted()) {
            Sort s = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), s);
        }

        String kw = normalize(req.getKeyword());
        String action = emptyToNull(req.getAction());
        String status = emptyToNull(req.getStatus());

        Page<AuditLog> page = auditLogRepository.search(kw, action, status, pageable);
        return page.map(AuditLogResDto::from);
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

}
