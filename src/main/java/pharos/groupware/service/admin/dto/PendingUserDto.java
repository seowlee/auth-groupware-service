package pharos.groupware.service.admin.dto;


import lombok.Data;

@Data
public class PendingUserDto {
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String provider; // kakao 등
}
