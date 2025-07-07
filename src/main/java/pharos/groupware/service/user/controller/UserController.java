package pharos.groupware.service.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "👤 사용자 기능", description = "용자 개인 정보 관련 API")
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/user")
public class UserController {
    
    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 이름, 이메일, 소속 팀 등의 정보를 확인합니다.")
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile() {
        // TODO: 사용자 프로필 조회
        return ResponseEntity.ok("내 프로필");
    }

    @Operation(summary = "내 알림 목록 조회", description = "시스템 알림, 연차 승인 알림 등을 확인합니다.")
    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications() {
        // TODO: 알림 조회
        return ResponseEntity.ok("알림 목록");
    }

}
