package pharos.groupware.service.domain.leave.controller;

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
import pharos.groupware.service.domain.leave.dto.CreateLeaveReqDto;
import pharos.groupware.service.domain.leave.dto.LeaveDetailResDto;
import pharos.groupware.service.domain.leave.dto.LeaveSearchReqDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveReqDto;
import pharos.groupware.service.domain.leave.service.LeaveService;

@Tag(name = "05. 연차 기능", description = "연차 신청, 조회, 수정, 취소 등 연차 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaves")
public class LeaveController {
    //    private final GraphUserService graphUserService;
    private final LeaveService leaveService;

    @Operation(summary = "연차 목록 조회", description = "조직 내 모든 사용자 연차 목록을 조회합니다.")
    @GetMapping()
    public ResponseEntity<PagedResponse<LeaveDetailResDto>> getAllLeaves(
            @ParameterObject @ModelAttribute LeaveSearchReqDto searchDto,
            @ParameterObject @PageableDefault(size = 5) Pageable pageable) {
        Page<LeaveDetailResDto> response = leaveService.getAllLeaves(searchDto, pageable);
        return ResponseEntity.ok(new PagedResponse<>(response));
    }


//    @Operation(summary = "특정 사용자의 연차 목록 조회", description = "지정된 사용자 연차 목록을 조회합니다.")
//    @GetMapping("/user/{userUuid}")
//    public ResponseEntity<PagedResponse<LeaveDetailResDto>> getUserLeaves() {
//        // TODO: SELECT * FROM leaves WHERE user_id = me
//        return ResponseEntity.ok("내 연차 목록");
//    }

    @Operation(summary = "연차 상세 조회", description = "ID로 연차 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveDetailResDto> getLeave(@PathVariable Long id) {
        LeaveDetailResDto dto = leaveService.getLeaveDetail(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "연차 신청", description = "새로운 연차를 신청합니다.")
    @PostMapping
    public ResponseEntity<String> applyLeave(@RequestBody CreateLeaveReqDto reqDto) {
        Long leaveId = leaveService.applyLeave(reqDto);
        return ResponseEntity.ok("연차 신청 완료");
    }

    @Operation(summary = "연차 수정", description = "기존 연차 정보를 수정합니다.")
    @PostMapping("/{id}")
    public ResponseEntity<String> updateLeave(@PathVariable Long id, @RequestBody UpdateLeaveReqDto reqDto) {
        leaveService.updateLeave(id, reqDto);
        return ResponseEntity.ok("연차 수정 완료");
    }

    @Operation(summary = "연차 취소", description = "기존 연차 신청을 취소합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelLeave(@PathVariable("id") Long id) {
        leaveService.cancelLeave(id);
        return ResponseEntity.ok("연차 취소 완료");
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
}
