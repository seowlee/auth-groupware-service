package pharos.groupware.service.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pharos.groupware.service.team.domain.Team;
import pharos.groupware.service.team.domain.TeamRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;


    @Override
    public List<Team> findAllTeams() {
        return teamRepository.findAll();

    }
}
