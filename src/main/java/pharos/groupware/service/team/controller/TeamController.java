package pharos.groupware.service.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.team.domain.Team;
import pharos.groupware.service.team.service.TeamService;

import java.util.List;

@Tag(name = "06. 팀 기능", description = "팀 목록, 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {
    private final TeamService teamService;

    @Operation(summary = "팀 목록 조회", description = "조직 내 모든 팀 정보를 조회합니다.")
    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamService.findAllTeams();
        return ResponseEntity.ok(teams);
    }
}
