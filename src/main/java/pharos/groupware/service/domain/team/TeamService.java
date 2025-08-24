package pharos.groupware.service.domain.team;

import pharos.groupware.service.domain.team.dto.TeamDto;

import java.util.List;

public interface TeamService {
    List<TeamDto> findAllTeams();
}
