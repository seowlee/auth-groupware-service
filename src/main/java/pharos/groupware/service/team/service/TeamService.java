package pharos.groupware.service.team.service;

import pharos.groupware.service.team.dto.TeamDto;

import java.util.List;

public interface TeamService {
    List<TeamDto> findAllTeams();
}
