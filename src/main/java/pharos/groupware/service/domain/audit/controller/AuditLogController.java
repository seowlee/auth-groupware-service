package pharos.groupware.service.domain.audit.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.common.page.PagedResponse;
import pharos.groupware.service.domain.audit.dto.AuditLogResDto;
import pharos.groupware.service.domain.audit.dto.AuditLogSearchReq;
import pharos.groupware.service.domain.audit.service.AuditLogService;

@Tag(name = "98. 감사 로그 API", description = "로그인 성공/실패 등 인증 감사 기록 조회")
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "감사 로그 목록", description = "감사 로그를 조건/페이지로 조회합니다.")
    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogResDto>> getLogs(
            @ParameterObject @ModelAttribute AuditLogSearchReq req,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        Page<AuditLogResDto> page = auditLogService.getLogs(req, pageable);
        return ResponseEntity.ok(new PagedResponse<>(page));
    }
}
