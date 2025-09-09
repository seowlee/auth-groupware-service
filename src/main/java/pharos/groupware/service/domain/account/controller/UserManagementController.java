package pharos.groupware.service.domain.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.common.annotation.RequireSuperAdmin;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.PendingUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.account.dto.UserApplicantResDto;
import pharos.groupware.service.domain.account.service.UserManagementService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "01. 사용자 통합 관리", description = "Keycloak / Graph / 로컬 사용자 등록 및 삭제 등 통합 사용자 생성 관리")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @RequireSuperAdmin
    @Operation(
            summary = "신규 사용자 등록",
            description = "Keycloak, Microsoft Graph, 로컬 DB에 신규 사용자를 생성합니다. SUPER_ADMIN만 사용 가능합니다."
    )
    public ResponseEntity<String> registerUser(@RequestBody CreateUserReqDto reqDTO) {
        String userUUID = AuthUtils.extractUserUUID();
        log.info("요청자 UUID: {}, 생성할 사용자: {}", userUUID, reqDTO.getUsername());
        String newUserId = userManagementService.createUser(reqDTO);
        if (newUserId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("사용자 생성 실패 (Keycloak 등 외부 시스템 오류)");
        }
        URI location = URI.create("/api/users/" + newUserId);
        return ResponseEntity.created(location).body(newUserId);
    }

    @Operation(summary = "대기 사용자 등록 (keycloak에서 호출)", description = "Keycloak 계정 없는 소셜로그인 사용자를 대기상태로 추가합니다.")
    @PostMapping("/pending")
    public ResponseEntity<Void> registerPendingUser(@RequestBody PendingUserReqDto dto) {
        log.info("신규 카카오 사용자 등록 요청: {}", dto);
        userManagementService.registerOrLinkSocialUser(dto);
        return ResponseEntity.ok().build();
    }


    @RequireSuperAdmin
    @Operation(summary = "사용자 삭제", description = "사용자를 Keycloak/graph에서 삭제합니다.")
    @PostMapping("/{keycloakUserId}")
    public ResponseEntity<Void> deleteKeycloakUser(@PathVariable("keycloakUserId") String keycloakUserId) {
        String userUUID = AuthUtils.extractUserUUID();
        userManagementService.deleteUser(keycloakUserId);
        return ResponseEntity.noContent().build();
    }


    @RequireSuperAdmin
    @Operation(summary = "관리자에 의한 사용자 정보 수정", description = "사용자 정보 수정/비활성화/대기 사용자 승인")
    @PostMapping("/{uuid}/update")
    public ResponseEntity<String> updateUser(@PathVariable("uuid") UUID uuid, @RequestBody UpdateUserByAdminReqDto reqDto, Authentication authentication) {
        String userUuid = userManagementService.updateUser(uuid, reqDto);
        return ResponseEntity.ok(userUuid);
    }


    @Operation(summary = "신청자 선택용 전체 목록", description = "연차 신청 시 선택 가능한 사용자 목록. SUPER_ADMIN 전용")
    @GetMapping("/applicants")
    public ResponseEntity<List<UserApplicantResDto>> getApplicants(
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(userManagementService.findAllApplicants(q));
    }

//    @Operation(summary = "사용자 Kakao IdP 연결", description = "지정 Keycloak userId에 Kakao를 federated identity로 등록합니다")
//    @PostMapping("/{userId}/link-kakao")
//    public ResponseEntity<Void> linkKakaoFederatedIdentity(
//            @PathVariable String userId,
//            @RequestBody LinkKakaoIdpReqDto dto
//    ) {
//        keycloakUserService.linkKakaoFederatedIdentity(
//                userId,
//                dto.getKakaoUserId(),
//                dto.getKakaoUsername()
//        );
//        return ResponseEntity.ok().build();
//    }
}
