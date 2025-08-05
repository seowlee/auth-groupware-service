package pharos.groupware.service.team.dto;


import pharos.groupware.service.team.domain.Team;

public record TeamDto(Long id, String name) {
    public static TeamDto from(Team team) {
        return new TeamDto(team.getId(), team.getName());
    }
}
