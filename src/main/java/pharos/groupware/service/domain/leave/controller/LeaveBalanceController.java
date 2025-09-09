package pharos.groupware.service.domain.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.dto.UpdateLeaveBalanceReqDto;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;

import java.util.List;
import java.util.UUID;

@Tag(name = "05. 연차 기능", description = "연차 신청, 조회, 수정, 취소 등 연차 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaves/balances")
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;

    @Operation(summary = "사용자 연차 보유 현황 조회")
    @GetMapping("/{uuid}")
    public ResponseEntity<List<LeaveBalanceResDto>> getLeaveBalances(@PathVariable UUID uuid) {
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalances(uuid));
    }

    @Operation(summary = "사용자 연차 보유 현황 수정")
    @PostMapping("/users/{uuid}")
    public ResponseEntity<Void> updateLeaveBalances(@PathVariable UUID uuid, @RequestBody @Valid List<UpdateLeaveBalanceReqDto> reqDto) {
        leaveBalanceService.update(uuid, reqDto);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "부서별 연차 통계 조회", description = "부서별 연차 사용 통계를 확인합니다.")
    @GetMapping("/stats/team/{teamId}")
    public String getLeaveStatsByTeam(@PathVariable("teamId") Long teamId) {
        // TODO: 통계 로직
        return "팀 통계";
    }
}
