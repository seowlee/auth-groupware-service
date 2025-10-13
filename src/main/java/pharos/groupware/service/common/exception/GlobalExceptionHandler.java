package pharos.groupware.service.common.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        // 로그는 상세
        log.warn("DataIntegrityViolation", e);

        String constraint = extractConstraintName(e);       // 예: uq_users_phone
        String field = mapConstraintToField(constraint);    // 예: phoneNumber
        String message = buildDuplicateMessage(field);      // 예: "휴대전화(활성 사용자) 값이 이미 사용 중입니다."

        // 일관된 페이로드 형식 (기존 핸들러들 스타일과 유사)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", message,
                "field", field,
                "constraint", constraint != null ? constraint : "unknown"
        ));
    }

    // 범용 예외 핸들러 (catch-all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        // 로그는 상세히 남김
        log.error("Unhandled exception caught in GlobalExceptionHandler", e);

        // 프론트에는 일반 메시지 전달
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }
    // ───────── helpers ─────────

    // 제약명 추출: Hibernate ConstraintViolationException 우선
    private String extractConstraintName(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return cve.getConstraintName(); // ex) uq_users_phone
            }
            cur = cur.getCause();
        }
        // 드라이버/DB 메시지 안에서 제약명이 노출되는 경우가 있으면 여기서 추가 파싱 가능
        return null;
    }

    // 제약명 → 필드 매핑 (스키마 네이밍에 맞춰 간단 매핑)
    private String mapConstraintToField(String constraint) {
        if (constraint == null) return "unknown";
        String c = constraint.toLowerCase();
        if (c.contains("username")) return "username";
        if (c.contains("email")) return "email";
        if (c.contains("phone")) return "phoneNumber";
        return "unknown";
    }

    private String buildDuplicateMessage(String field) {
        String label = switch (field) {
            case "username" -> "사용자명";
            case "email" -> "이메일";
            case "phoneNumber" -> "휴대전화";
            default -> "값";
        };
        // 현재 제약이 (field, status) 복합 유니크이므로 사용자 친화적으로 설명
        // 필요하면 status를 동적으로 포함할 수 있으나, 메시지 단순화를 위해 고정 문구 사용
        return label + "(활성 사용자) 값이 이미 사용 중입니다.";
    }
}
