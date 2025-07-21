package pharos.groupware.service.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.admin.service.UserManagementService;
import pharos.groupware.service.common.annotation.RequireSuperAdmin;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;

import java.net.URI;
import java.util.UUID;

@Tag(name = "01. 사용자 통합 관리", description = "Keycloak / Graph / 로컬 사용자 등록 및 삭제 등 통합 사용자 생성 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final UserRepository userRepository;

    @RequireSuperAdmin
    @PostMapping
    @Operation(
            summary = "신규 사용자 등록",
            description = "Keycloak, Microsoft Graph, 로컬 DB에 신규 사용자를 생성합니다. SUPER_ADMIN만 사용 가능합니다."
    )
    public ResponseEntity<String> registerUser(@RequestBody CreateUserReqDto reqDTO, Authentication authentication) {
        String userUUID = AuthUtils.extractUserUUID(authentication);

        String newUserId = userManagementService.createUser(reqDTO);
        URI location = URI.create("/api/users/" + newUserId);
        return ResponseEntity.created(location).body(newUserId);
    }

    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다.")
    @PostMapping("/{userId}/deactivate")
    public String deactivateUser(@PathVariable("userId") String userId) {
        userManagementService.deactivateUser(userId);
        return "OK";
    }

    @RequireSuperAdmin
    @Operation(summary = "사용자 삭제", description = "사용자를 Keycloak/graph에서 삭제합니다.")
    @PostMapping("/{userId}")
    public ResponseEntity<Void> deleteKeycloakUser(@PathVariable("userId") String keycloakUserId, Authentication authentication) {
        String userUUID = AuthUtils.extractUserUUID(authentication);

        User user = userRepository.findByUserUuid(UUID.fromString(keycloakUserId))
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        userManagementService.deleteUser(user.getUserUuid().toString(), user.getGraphUserId());
        return ResponseEntity.noContent().build();
    }

}
