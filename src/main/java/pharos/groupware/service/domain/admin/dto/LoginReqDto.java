package pharos.groupware.service.domain.admin.dto;

import lombok.Data;

@Data
public class LoginReqDto {
    private String username;
    private String password;
}
