package pharos.groupware.service.domain.leave.service;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LeavePolicyService {
    // ── 겹침(Overlap) 정책 ──
    void assertNoOverlapOrThrow(UUID userUuid, LocalDateTime start, LocalDateTime end);

    void assertNoOverlapOrThrow(UUID userUuid, Long excludeLeaveId, LocalDateTime start, LocalDateTime end);
    
}
