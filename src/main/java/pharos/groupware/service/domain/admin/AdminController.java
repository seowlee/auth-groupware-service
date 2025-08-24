package pharos.groupware.service.domain.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.domain.account.dto.LoginReqDto;
import pharos.groupware.service.domain.account.dto.LoginResDto;
import pharos.groupware.service.domain.account.service.AdminService;

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


}
