package pharos.groupware.service.domain.team.dto;

import lombok.Data;
import pharos.groupware.service.domain.team.entity.User;

@Data
public class UserResDto {
    private String uuid;
    private String username;
    private String email;
    private String role;
    private String joinedDate;
    private String status;
    private Long teamId;
    private String teamName;

    public static UserResDto fromEntity(User user) {
        UserResDto dto = new UserResDto();
        dto.setUuid(user.getUserUuid().toString());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setJoinedDate(user.getJoinedDate().toString()); // ISO 8601 기본 포맷
        dto.setTeamId(user.getTeam().getId());
        dto.setTeamName(user.getTeam().getKrName());
        return dto;
    }
}
