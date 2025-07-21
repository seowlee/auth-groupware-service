package pharos.groupware.service.audit.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.audit.service.AuditLogService;

@Tag(name = "98. 감사 로그 API", description = "로그인 성공/실패 등 인증 감사 기록 조회")
@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN') or hasRole('MASTER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "최근 로그인 기록 조회", description = "최근 로그인 시도 내역을 성공/실패 포함하여 조회합니다.")
    @GetMapping("/logins")
    public ResponseEntity<?> getRecentLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
//        return ResponseEntity.ok(auditLogService.getLoginLogs(page, size));
        return ResponseEntity.ok("ok");
    }
}
