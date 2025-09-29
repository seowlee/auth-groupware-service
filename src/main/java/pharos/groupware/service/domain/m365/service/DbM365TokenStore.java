package pharos.groupware.service.domain.m365.service;

import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.domain.m365.dto.SaveDelegatedTokenCommandDto;
import pharos.groupware.service.domain.m365.entity.M365OAuthToken;
import pharos.groupware.service.domain.m365.entity.M365OAuthTokenRepository;

import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
public class DbM365TokenStore implements M365TokenStore {

    private static final String TOKEN_KEY = "TENANT_DELEGATED";

    private final M365OAuthTokenRepository repo;
    private final StringEncryptor encryptor;


    @Override
    @Transactional(readOnly = true)
    public Optional<String> loadRefreshToken() {
        return repo.findByTokenKey(TOKEN_KEY)
                .map(M365OAuthToken::getRefreshTokenCipher)
                .map(encryptor::decrypt);
    }

    @Override
    @Transactional
    public void saveRefreshToken(String refreshTokenPlain) {
        String cipher = encryptor.encrypt(refreshTokenPlain);

        M365OAuthToken entity = repo.findByTokenKey(TOKEN_KEY)
                .map(e -> {
                    e.rotateRefreshTokenCipher(cipher, "system");
                    return e;
                })
                .orElseGet(() -> M365OAuthToken.create(TOKEN_KEY, cipher));

        repo.save(entity);
    }

    @Override
    @Transactional
    public void saveDelegatedToken(SaveDelegatedTokenCommandDto cmd) {
        String cipher = encryptor.encrypt(cmd.getRefreshTokenPlain());

        repo.findByTokenKey(TOKEN_KEY)
                .ifPresentOrElse(e -> {
                    e.rotateRefreshTokenCipher(cipher, cmd.getByName());
                }, () -> {
                    M365OAuthToken created = M365OAuthToken.create(
                            TOKEN_KEY,
                            cipher,
                            cmd.getTenantId(),
                            cmd.getClientId(),
                            cmd.getScope(),
                            cmd.getByUuid(),
                            cmd.getByName()
                    );
                    repo.save(created);
                });
    }

}
