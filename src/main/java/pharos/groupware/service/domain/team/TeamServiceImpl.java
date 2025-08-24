package pharos.groupware.service.domain.team;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.domain.team.dto.TeamDto;
import pharos.groupware.service.domain.team.entity.TeamRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;


    @Override
    public List<TeamDto> findAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(TeamDto::from)
                .toList();

    }
}
