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
import pharos.groupware.service.common.annotation.CurrentActor;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.page.PagedResponse;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.domain.leave.dto.*;
import pharos.groupware.service.domain.leave.service.LeaveService;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static pharos.groupware.service.common.util.DateUtils.KST;

@Tag(name = "05. 연차 기능", description = "연차 신청, 조회, 수정, 취소 등 연차 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaves")
public class LeaveController {
    //    private final GraphUserService graphUserService;
    private final LeaveService leaveService;

    private static OffsetDateTime parseToOffset(String s, boolean startOfDayIfDateOnly) {
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception ignore) {
            LocalDate d = LocalDate.parse(s);
            ZoneId KST = ZoneId.of("Asia/Seoul");
            return startOfDayIfDateOnly
                    ? d.atStartOfDay(KST).toOffsetDateTime()
                    : d.atTime(LocalTime.MAX).atZone(KST).toOffsetDateTime();
        }
    }


//    @Operation(summary = "특정 사용자의 연차 목록 조회", description = "지정된 사용자 연차 목록을 조회합니다.")
//    @GetMapping("/user/{userUuid}")
//    public ResponseEntity<PagedResponse<LeaveDetailResDto>> getUserLeaves() {
//        // TODO: SELECT * FROM leaves WHERE user_id = me
//        return ResponseEntity.ok("내 연차 목록");
//    }

    @Operation(summary = "연차 목록 조회", description = "조직 내 모든 사용자 연차 목록을 조회합니다.")
    @GetMapping()
    public ResponseEntity<PagedResponse<LeaveDetailResDto>> getAllLeaves(
            @ParameterObject @ModelAttribute LeaveSearchReqDto searchDto,
            @ParameterObject @PageableDefault(size = 5) Pageable pageable,
            @CurrentActor AppUser actor) {
        // 기본값: null이면 true로
        if (searchDto.getMyOnly() == null) {
            searchDto.setMyOnly(Boolean.TRUE);
        }
        Page<LeaveDetailResDto> response = leaveService.getAllLeaves(searchDto, pageable, actor);
        return ResponseEntity.ok(new PagedResponse<>(response));
    }

    @Operation(summary = "연차 상세 조회", description = "ID로 연차 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<LeaveDetailResDto> getLeave(@PathVariable Long id) {
        LeaveDetailResDto dto = leaveService.getLeaveDetail(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "연차 신청", description = "새로운 연차를 신청합니다.")
    @PostMapping
    public ResponseEntity<ApplyLeaveResDto> applyLeave(@RequestBody CreateLeaveReqDto reqDto, @CurrentActor AppUser actor) {
        Long leaveId = leaveService.applyLeave(reqDto, actor);
        URI location = URI.create("/api/leaves/" + leaveId);
        return ResponseEntity.created(location)
                .body(new ApplyLeaveResDto(leaveId, "연차 신청 완료"));
    }

    @Operation(summary = "연차 수정", description = "기존 연차 정보를 수정합니다.")
    @PostMapping("/{id}")
    public ResponseEntity<String> updateLeave(@PathVariable Long id, @RequestBody UpdateLeaveReqDto reqDto, @CurrentActor AppUser actor) {
        Long leaveId = leaveService.updateLeave(id, reqDto, actor);
        return ResponseEntity.ok("연차 수정 완료");
    }

    @Operation(summary = "연차 취소", description = "기존 연차 신청을 취소합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancelLeave(@PathVariable("id") Long id, @CurrentActor AppUser actor) {
        leaveService.cancelLeave(id, actor);
        return ResponseEntity.ok("연차 취소 완료");
    }


    @Operation(summary = "캘린더 연차 목록 조회", description = "캘린더에 표시될 조직 내 모든 사용자 연차 목록을 조회합니다.")
    @GetMapping("/calendar")
    public List<CalendarEventResDto> getCalendarEvents(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        LeaveTypeEnum typeEnum = (type == null || type.isBlank()) ? null : LeaveTypeEnum.valueOf(type);
        LeaveStatusEnum statusEnum = (status == null || status.isBlank()) ? null : LeaveStatusEnum.valueOf(status);

        // OffsetDateTime 우선 파싱, 실패 시 LocalDate 경계로 보정
        LocalDate base = LocalDate.now(KST);
        String startStr = (start == null || start.isBlank())
                ? base.minusMonths(1).toString()   // "yyyy-MM-dd"
                : start;

        String endStr = (end == null || end.isBlank())
                ? base.plusMonths(1).toString()    // "yyyy-MM-dd"
                : end;
        OffsetDateTime from = parseToOffset(startStr, true);
        OffsetDateTime to = parseToOffset(endStr, false);

        return leaveService.getCalendarEvents(teamId, typeEnum, statusEnum, from, to);
    }

    //    @Operation(summary = "사용자 연차 승인", description = "특정 연차 신청을 승인 처리합니다.")
//    @PostMapping("/leaves/{id}/approve")
//    public ResponseEntity<?> approveLeave(@PathVariable("id") Long id) {
//        // TODO: UPDATE leaves SET status = 'APPROVED' WHERE id = ...
//        return ResponseEntity.ok("연차 승인 완료");
//    }

//    @Operation(summary = "사용자 연차 거절", description = "특정 연차 신청을 거절 처리합니다.")
//    @PostMapping("/leaves/{id}/reject")
//    public ResponseEntity<?> rejectLeave(@PathVariable("id") Long id) {
//        // TODO: UPDATE leaves SET status = 'REJECTED' WHERE id = ...
//        return ResponseEntity.ok("연차 거절 완료");
//    }
}
