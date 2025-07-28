package pharos.groupware.service.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;

import java.time.LocalDate;

@Data
public class CreateIdpUserReqDto {
    private String userUUID;
    private String username;
    private String rawPassword;
    private String email;
    private String firstName;
    private String lastName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinedDate;
    private UserRoleEnum role;
    private UserStatusEnum status;
}
