package pharos.groupware.service.domain.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.common.page.PagedResponse;
import pharos.groupware.service.domain.account.dto.UpdateUserProfileReqDto;
import pharos.groupware.service.domain.team.dto.UserDetailResDto;
import pharos.groupware.service.domain.team.dto.UserResDto;
import pharos.groupware.service.domain.team.dto.UserSearchReqDto;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.util.UUID;

@Tag(name = "03. 사용자 계정 정보", description = "사용자 목록, 개별 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class UserController {
    private final UserService userService;
    private final KeycloakUserService keycloakUserService;

    //    {
//        "graphUserId": "leader1@gwco.onmicrosoft.com",
//            "subject": "회의 알림",
//            "bodyContent": "팀 전체 회의입니다.",
//            "startDateTime": "2025-07-21T10:00:00",
//            "endDateTime": "2025-07-21T11:00:00",
//            "timezone": "Asia/Seoul"
//    }

    @Operation(summary = "전체 사용자 조회", description = "조직 내 모든 사용자 정보를 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserResDto>> getAllUsers(
            @ParameterObject @ModelAttribute UserSearchReqDto userSearchReqDto,
            @ParameterObject @PageableDefault(size = 5) Pageable pageable) {
        Page<UserResDto> userPage = userService.findAllUsers(userSearchReqDto, pageable);
        return ResponseEntity.ok(new PagedResponse<>(userPage));
    }

    @Operation(summary = "단일 사용자 조회")
    @GetMapping("/users/{uuid}")
    public ResponseEntity<UserDetailResDto> getUserDetail(
            @PathVariable UUID uuid, @RequestParam(defaultValue = "false") boolean includeBalances) {
        UserDetailResDto resDto = userService.getUserDetail(uuid, includeBalances);
        return ResponseEntity.ok(resDto);
    }

    @Operation(summary = "사용자 내 정보 수정")
    @PutMapping("/users/{uuid}")
    public ResponseEntity<Void> updateUser(@PathVariable UUID uuid,
                                           @RequestBody UpdateUserProfileReqDto reqDto) {
//        userService.updateUser(uuid, reqDto);
        return ResponseEntity.ok().build();
    }


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
