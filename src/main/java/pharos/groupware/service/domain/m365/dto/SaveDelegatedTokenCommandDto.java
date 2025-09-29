package pharos.groupware.service.domain.m365.dto;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveDelegatedTokenCommandDto {
    String refreshTokenPlain; // 필수
    String tenantId;          // 선택(감사용)
    String clientId;          // 선택
    String scope;             // 선택

    String byUuid;            // 연결 실행자(선택)
    String byName;            // 연결 실행자 이름(선택)
}
