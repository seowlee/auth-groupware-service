package pharos.groupware.service.domain.m365.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthTokenResDto {
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Long expiresIn;
    @JsonProperty("ext_expires_in")
    private Long extExpiresIn;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("scope")
    private String scope;
}
