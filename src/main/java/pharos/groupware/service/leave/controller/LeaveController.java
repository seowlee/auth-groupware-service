package pharos.groupware.service.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "🌴 연차 기능", description = "연차 신청, 조회, 수정, 취소 등 연차 관련 API")
@RestController
@RequestMapping("/leaves")
@PreAuthorize("hasRole('USER')")
public class LeaveController {

    @Operation(summary = "내 연차 목록 조회", description = "내가 신청한 연차 목록을 상태 정보와 함께 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves() {
        // TODO: SELECT * FROM leaves WHERE user_id = me
        return ResponseEntity.ok("내 연차 목록");
    }

    @Operation(summary = "연차 신청", description = "새로운 연차를 신청합니다.")
    @PostMapping
    public ResponseEntity<?> applyLeave(@RequestBody Object leaveRequest) {
        // TODO: INSERT INTO leaves ...
        return ResponseEntity.ok("연차 신청 완료");
    }

    @Operation(summary = "연차 수정", description = "기존 연차 정보를 수정합니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateLeave(@PathVariable("id") Long id, @RequestBody Object leaveUpdate) {
        // TODO: UPDATE leaves SET ...
        return ResponseEntity.ok("연차 수정 완료");
    }

    @Operation(summary = "연차 취소", description = "기존 연차 신청을 취소합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelLeave(@PathVariable("id") Long id) {
        // TODO: DELETE or UPDATE 상태 → 취소됨
        return ResponseEntity.ok("연차 취소 완료");
    }
}
