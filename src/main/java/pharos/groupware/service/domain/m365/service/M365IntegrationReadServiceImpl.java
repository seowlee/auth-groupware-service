package pharos.groupware.service.domain.m365.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class M365IntegrationReadServiceImpl implements M365IntegrationReadService {
    private final M365TokenStore tokenStore;

    @Override
    @Transactional(readOnly = true)
    public boolean isLinked() {
        // 복호화된 refresh token이 존재/비어있지 않으면 연결된 것으로 간주
        return tokenStore.loadRefreshToken()
                .filter(s -> !s.isBlank())
                .isPresent();
    }
}
