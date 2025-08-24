package pharos.groupware.service.domain.account.dto;

import lombok.Data;

@Data
public class LinkKakaoIdpReqDto {
    private String kakaoUserId;    // Kakao 'sub'
    private String kakaoUsername;  // Kakao email
}