package pharos.groupware.service.domain.account.dto;


import lombok.Data;

@Data
public class PendingUserReqDto {
    private String email;
    private String username;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String provider; // kakao 등
    private String providerUserId;
}
