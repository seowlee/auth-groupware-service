package pharos.groupware.service.domain.m365.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface M365OAuthTokenRepository extends JpaRepository<M365OAuthToken, Long> {
    Optional<M365OAuthToken> findByTokenKey(String tokenKey);
}