package pharos.groupware.service.domain.team.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserDetailResDto {
    private UUID uuid;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String joinedDate;
    private String role;
    private String status;
    private Long teamId;
    private String teamName;
    private List<LeaveBalanceDto> leaveBalances;

    @Data
    public static class LeaveBalanceDto {
        private String typeName;
        private int remainingDays;
    }
}
