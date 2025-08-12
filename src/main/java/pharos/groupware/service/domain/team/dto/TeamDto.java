package pharos.groupware.service.domain.team.dto;


import pharos.groupware.service.domain.team.entity.Team;

public record TeamDto(Long id, String name) {
    public static TeamDto from(Team team) {
        return new TeamDto(team.getId(), team.getName());
    }
}
