package pharos.groupware.service.domain.m365.service;

import pharos.groupware.service.domain.m365.dto.SaveDelegatedTokenCommandDto;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

// NEW: 데모용 메모리 구현
//@Component
public class InMemoryM365TokenStore implements M365TokenStore {
    private final AtomicReference<String> rt = new AtomicReference<>();

    @Override
    public Optional<String> loadRefreshToken() {
        return Optional.ofNullable(rt.get());
    }

    @Override
    public void saveRefreshToken(String refreshToken) {
        rt.set(refreshToken);
    }

    @Override
    public void saveDelegatedToken(SaveDelegatedTokenCommandDto cmd) {

    }
}
