package pharos.groupware.service.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinedDate;

    @Schema(hidden = true)
    private int yearNumber;

    private UserRoleEnum role;

    private Long teamId;


}
