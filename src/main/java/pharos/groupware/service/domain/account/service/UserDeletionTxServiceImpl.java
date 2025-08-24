package pharos.groupware.service.domain.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionTxServiceImpl implements UserDeletionTxService {
    private final KeycloakUserService keycloakUserService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteKeycloakOnly(String keycloakUserId) {
        try {
            keycloakUserService.deleteUser(keycloakUserId);
        } catch (HttpClientErrorException.NotFound e) {
            // 없으면 이미 삭제된 것으로 간주
            log.warn("Keycloak user not found (treat as deleted). userid={}", keycloakUserId);
        }
    }
}
