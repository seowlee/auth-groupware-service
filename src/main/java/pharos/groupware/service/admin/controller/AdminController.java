package pharos.groupware.service.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.admin.dto.LoginReqDto;
import pharos.groupware.service.admin.dto.LoginResDto;
import pharos.groupware.service.admin.service.AdminService;

import java.util.Map;


@Tag(name = "02. 최고 관리자", description = "Fallback 로그인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @Operation(
            summary = "Local 로그인 (Fallback)",
            description = "Keycloak 서비스 장애 시 사용할 최고관리자 전용 로그인 API 입니다."
    )
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginReqDto reqDto,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        LoginResDto resDto = adminService.login(reqDto, request, response);
        return ResponseEntity.ok(Map.of(
                "accessToken", resDto.getAccessToken(),
                "redirectUrl", "/home"
        ));
    }


    @Operation(summary = "관리자 본인 정보 조회", description = "현재 로그인한 관리자 정보를 확인합니다.")
    @GetMapping("/profile")
    public ResponseEntity<?> getAdminProfile() {
        // TODO: SELECT * FROM users WHERE id = admin
        return ResponseEntity.ok("관리자 프로필");
    }
}
