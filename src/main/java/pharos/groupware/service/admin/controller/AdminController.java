package pharos.groupware.service.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "🛠 관리자 기능", description = "연차 승인/거절, 사용자 관리, 통계 관련 API")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Operation(summary = "전체 사용자 조회", description = "조직 내 모든 사용자 정보를 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        // TODO: SELECT * FROM users
        return ResponseEntity.ok("전체 사용자 목록");
    }

    @Operation(summary = "사용자 연차 승인", description = "특정 연차 신청을 승인 처리합니다.")
    @PostMapping("/leaves/{id}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable("id") Long id) {
        // TODO: UPDATE leaves SET status = 'APPROVED' WHERE id = ...
        return ResponseEntity.ok("연차 승인 완료");
    }

    @Operation(summary = "사용자 연차 거절", description = "특정 연차 신청을 거절 처리합니다.")
    @PostMapping("/leaves/{id}/reject")
    public ResponseEntity<?> rejectLeave(@PathVariable("id") Long id) {
        // TODO: UPDATE leaves SET status = 'REJECTED' WHERE id = ...
        return ResponseEntity.ok("연차 거절 완료");
    }

    @Operation(summary = "부서별 연차 통계 조회", description = "부서별 연차 사용 통계를 확인합니다.")
    @GetMapping("/stats/team/{teamId}")
    public String getLeaveStatsByTeam(@PathVariable("teamId") Long teamId) {
        // TODO: 통계 로직
        return "팀 통계";
    }

    @Operation(summary = "관리자 본인 정보 조회", description = "현재 로그인한 관리자 정보를 확인합니다.")
    @GetMapping("/profile")
    public ResponseEntity<?> getAdminProfile() {
        // TODO: SELECT * FROM users WHERE id = admin
        return ResponseEntity.ok("관리자 프로필");
    }
}
