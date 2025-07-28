package pharos.groupware.service.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "04. 일정 기능", description = "조직 및 팀 일정 캘린더 조회 API")
@RestController
@RequestMapping("/schedules")
public class ScheduleController {
    @Operation(summary = "팀 일정 캘린더 조회", description = "특정 팀의 구성원 일정을 캘린더 형식으로 조회합니다. (Graph API 또는 DB 기반)")
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTeamSchedule(
            @PathVariable("teamId") Long teamId,
            @RequestParam String start,
            @RequestParam String end
    ) {
        // TODO: Graph API /users/calendarView 또는 DB 조회
        return ResponseEntity.ok("팀 일정 캘린더");
    }

    @Operation(summary = "조직 전체 일정 조회", description = "조직 전체의 공용 일정을 캘린더 형식으로 조회합니다.")
    @GetMapping("/organization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrganizationSchedule(
            @RequestParam String start,
            @RequestParam String end
    ) {
        // TODO: Graph API 또는 DB 조회
        return ResponseEntity.ok("조직 전체 일정");
    }
}
