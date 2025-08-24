package pharos.groupware.service.domain.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserByAdminReqDto {
    private String phoneNumber;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinedDate;
    private Long teamId;
}
