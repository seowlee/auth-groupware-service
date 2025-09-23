package pharos.groupware.service.domain.team.dto;

import lombok.Data;
import pharos.groupware.service.domain.team.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class UserDetailResDto {
    private UUID uuid;
    private String username;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String joinedDate;
    private Integer yearNumber;
    private String role;
    private String status;
    private Long teamId;
    private String teamName;
    private List<LeaveBalanceDto> leaveBalances;

    public static UserDetailResDto fromEntity(User user) {
        UserDetailResDto dto = new UserDetailResDto();
        dto.setUuid(user.getUserUuid());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setJoinedDate(user.getJoinedDate().toString());
        dto.setYearNumber(user.getYearNumber());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setTeamId(user.getTeam().getId());
        dto.setTeamName(user.getTeam().getKrName());
        return dto;
    }

    @Data
    public static class LeaveBalanceDto {
        private String leaveType;
        private BigDecimal totalAllocated;
        private BigDecimal used;
        private Integer yearNumber;

    }
}
