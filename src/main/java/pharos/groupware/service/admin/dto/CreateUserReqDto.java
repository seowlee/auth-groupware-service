package pharos.groupware.service.admin.dto;

import lombok.Data;
import pharos.groupware.service.common.enums.UserRoleEnum;

import java.time.LocalDate;

@Data
public class CreateUserReqDto {
    private String userUUID;
    private String username;
    private String rawPassword;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate joinedDate;
    private UserRoleEnum role;
}
