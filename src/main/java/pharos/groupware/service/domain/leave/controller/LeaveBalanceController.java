package pharos.groupware.service.domain.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.domain.leave.dto.LeaveBalanceResDto;
import pharos.groupware.service.domain.leave.service.LeaveBalanceService;

import java.util.List;
import java.util.UUID;

@Tag(name = "03. 연차 기능", description = "연차 신청, 조회, 수정, 취소 등 연차 관련 API")
@RestController
@RequiredArgsConstructor
public class LeaveBalanceController {
    private final LeaveBalanceService leaveBalanceService;

    @Operation(summary = "사용자 연차 보유 현황 조회")
    @GetMapping("/balances/{uuid}")
    public ResponseEntity<List<LeaveBalanceResDto>> getLeaveBalances(@PathVariable UUID uuid) {
        return ResponseEntity.ok(leaveBalanceService.getLeaveBalances(uuid));
    }
}
