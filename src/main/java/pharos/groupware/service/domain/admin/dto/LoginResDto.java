package pharos.groupware.service.domain.admin.dto;

import lombok.Getter;

@Getter
public class LoginResDto {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";
    private final long expiresIn = 3600;

    public LoginResDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
