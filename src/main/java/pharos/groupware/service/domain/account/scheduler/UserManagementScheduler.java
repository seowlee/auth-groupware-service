package pharos.groupware.service.domain.account.scheduler;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pharos.groupware.service.domain.account.service.UserManagementService;

@Component
@RequiredArgsConstructor
public class UserManagementScheduler {
    private final UserManagementService userManagementService;

    @Operation(summary = "사용자 삭제 배치", description = "비활성 기준일 초과 사용자를 Keycloak/local에서 삭제합니다.")
    @Scheduled(cron = "0 28 3 * * *", zone = "Asia/Seoul")
    public ResponseEntity<Void> deleteUser() {
        userManagementService.deleteUsersOlderThanDays(0);
        return ResponseEntity.noContent().build();
    }
}
