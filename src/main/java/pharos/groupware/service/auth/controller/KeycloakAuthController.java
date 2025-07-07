package pharos.groupware.service.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.auth.service.KeycloakAuthService;

import java.net.URI;

@Tag(name = " Keycloak 관리 API", description = "Keycloak 사용자 등록/삭제 및 그룹 연동 등 관리자 기능 담당")
@RestController
@RequestMapping("/keycloak/auth")
public class KeycloakAuthController {

    private final KeycloakAuthService authService;

    public KeycloakAuthController(KeycloakAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Keycloak 사용자 생성", description = "신규 사용자를 Keycloak에 생성하고 그룹(role)을 부여합니다.")
    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<String> addKeycloakUser(@RequestBody CreateUserReqDTO reqDTO) {
        String newUserId = authService.createUser(reqDTO);
        URI location = URI.create("/api/users/" + newUserId);
        return ResponseEntity.created(location).body(newUserId);
    }

    @Operation(summary = "Keycloak 사용자 삭제", description = "사용자를 Keycloak에서 삭제합니다.")
    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('MASTER')")
    public String deleteKeycloakUser(@PathVariable("userId") String userId) {
        authService.deleteUser(userId);
        return "OK";
    }

    @Operation(summary = "IDP 로그인 사용자에게 그룹 부여", description = "소셜 로그인 사용자를 user-group에 추가")
    @PostMapping("/{userId}/assign-group")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<String> assignGroupToUser(@PathVariable String userId) {
//        authService.assignGroup(userId, "user-group");
        return ResponseEntity.ok("Group assigned");

    }
}
