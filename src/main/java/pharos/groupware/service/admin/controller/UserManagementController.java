package pharos.groupware.service.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.admin.dto.LinkKakaoIdpReqDto;
import pharos.groupware.service.admin.dto.PendingUserDto;
import pharos.groupware.service.admin.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.admin.service.UserManagementService;
import pharos.groupware.service.common.annotation.RequireSuperAdmin;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;
import pharos.groupware.service.team.service.UserService;

import java.net.URI;
import java.util.UUID;

@Tag(name = "01. 사용자 통합 관리", description = "Keycloak / Graph / 로컬 사용자 등록 및 삭제 등 통합 사용자 생성 관리")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final KeycloakUserService keycloakUserService;
    private final UserService localUserService;

    @PostMapping
    @RequireSuperAdmin
    @Operation(
            summary = "신규 사용자 등록",
            description = "Keycloak, Microsoft Graph, 로컬 DB에 신규 사용자를 생성합니다. SUPER_ADMIN만 사용 가능합니다."
    )
    public ResponseEntity<String> registerUser(@RequestBody CreateUserReqDto reqDTO, Authentication authentication) {
        String userUUID = AuthUtils.extractUserUUID(authentication);
        log.info("요청자 UUID: {}, 생성할 사용자: {}", userUUID, reqDTO.getUsername());
        String newUserId = userManagementService.createUser(reqDTO);
        if (newUserId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("사용자 생성 실패 (Keycloak 등 외부 시스템 오류)");
        }
        URI location = URI.create("/api/users/" + newUserId);
        return ResponseEntity.created(location).body(newUserId);
    }

    @PostMapping("/pending")
    public ResponseEntity<Void> registerPendingUser(@RequestBody PendingUserDto dto) {
        log.info("신규 카카오 사용자 등록 요청: {}", dto.getEmail());

        localUserService.registerPendingUser(dto);


        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다.")
    @PostMapping("/{keycloakUserId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable("keycloakUserId") String keycloakUserId, Authentication authentication) {
        String userUuid = userManagementService.deactivateUser(keycloakUserId);
        return ResponseEntity.ok(userUuid);
    }

    @RequireSuperAdmin
    @Operation(summary = "사용자 삭제", description = "사용자를 Keycloak/graph에서 삭제합니다.")
    @PostMapping("/{keycloakUserId}")
    public ResponseEntity<Void> deleteKeycloakUser(@PathVariable("keycloakUserId") String keycloakUserId, Authentication authentication) {
        String userUUID = AuthUtils.extractUserUUID(authentication);
        userManagementService.deleteUser(keycloakUserId);
        return ResponseEntity.noContent().build();
    }

    @RequireSuperAdmin
    @Operation(summary = "관리자에 의한 사용자 정보 수정", description = "최고관리자가 사용자 정보를 수정합니다.")
    @PostMapping("/{uuid}/update")
    public ResponseEntity<String> updateUser(@PathVariable("uuid") UUID uuid, @RequestBody UpdateUserByAdminReqDto reqDto, Authentication authentication) {
        String userUuid = userManagementService.updateUser(uuid, reqDto);
        return ResponseEntity.ok(userUuid);
    }

    /**
     * 로컬에서 PENDING 상태인 사용자를 승인하고
     * Keycloak에도 User 생성 → Kakao IdP 연동까지 수행합니다.
     */
    @PostMapping("/{userUuid}/approve")
    public ResponseEntity<Void> approvePending(
            @PathVariable UUID userUuid
    ) {
        // userUuid 만 넘겨주면 내부에서 kakaoSub/kakaoUsername은 엔티티에 저장된 값을 꺼내 씁니다.
        userManagementService.approvePendingUser(userUuid);
        return ResponseEntity.ok().build();
    }

//    @Operation(summary = "사용자 연차 승인", description = "특정 연차 신청을 승인 처리합니다.")
//    @PostMapping("/leaves/{id}/approve")
//    public ResponseEntity<?> approveLeave(@PathVariable("id") Long id) {
//        // TODO: UPDATE leaves SET status = 'APPROVED' WHERE id = ...
//        return ResponseEntity.ok("연차 승인 완료");
//    }

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

    @PostMapping("/{userId}/link-kakao")
    public ResponseEntity<Void> linkKakaoFederatedIdentity(
            @PathVariable String userId,
            @RequestBody LinkKakaoIdpReqDto dto
    ) {
        keycloakUserService.linkKakaoFederatedIdentity(
                userId,
                dto.getKakaoUserId(),
                dto.getKakaoUsername()
        );
        return ResponseEntity.ok().build();
    }


}
