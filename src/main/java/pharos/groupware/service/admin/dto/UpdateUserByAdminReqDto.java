package pharos.groupware.service.admin.dto;

import lombok.Data;

@Data
public class UpdateUserByAdminReqDto {
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private Long teamId;
//    private Map<String, BigDecimal> leaveTypeToCountMap;
}
