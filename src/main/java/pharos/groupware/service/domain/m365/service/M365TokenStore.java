package pharos.groupware.service.domain.m365.service;

import pharos.groupware.service.domain.m365.dto.SaveDelegatedTokenCommandDto;

import java.util.Optional;

// 토큰 저장/조회용 인터페이스
public interface M365TokenStore {
    Optional<String> loadRefreshToken();                 // 평문 반환

    void saveRefreshToken(String refreshTokenPlain);     // 평문 입력

    void saveDelegatedToken(SaveDelegatedTokenCommandDto cmd);
}
