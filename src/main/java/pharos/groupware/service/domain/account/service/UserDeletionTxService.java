package pharos.groupware.service.domain.account.service;

public interface UserDeletionTxService {
    void deleteKeycloakOnly(String keycloakUserId);
}
