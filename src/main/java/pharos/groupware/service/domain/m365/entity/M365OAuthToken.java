package pharos.groupware.service.domain.m365.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "m365_oauth_token", schema = "groupware")
public class M365OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단일 테넌트용 고정 키(예: "TENANT_DELEGATED")
     */
    @Column(name = "token_key", nullable = false, unique = true, length = 64)
    private String tokenKey;

    /**
     * 암호화된 refresh token 저장(평문 금지)
     */
    @Column(name = "refresh_token_cipher", nullable = false, columnDefinition = "text")
    private String refreshTokenCipher;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "client_id", length = 64)
    private String clientId;

    @Column(name = "scope", columnDefinition = "text")
    private String scope;

    @Column(name = "connected_by_uuid", length = 64)
    private String connectedByUuid;

    @Column(name = "connected_by_name", length = 100)
    private String connectedByName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public static M365OAuthToken create(String tokenKey, String cipher) {
        var e = new M365OAuthToken();
        e.tokenKey = tokenKey;
        e.refreshTokenCipher = cipher;
        return e;
    }

    public static M365OAuthToken create(
            String tokenKey, String cipher,
            String tenantId, String clientId, String scope,
            String byUuid, String byName
    ) {
        M365OAuthToken e = new M365OAuthToken();
        e.tokenKey = tokenKey;
        e.refreshTokenCipher = cipher;
        e.tenantId = tenantId;
        e.clientId = clientId;
        e.scope = scope;
        e.connectedByUuid = byUuid;
        e.connectedByName = byName;
        e.createdBy = (byName == null || byName.isBlank()) ? "system" : byName;
        e.updatedBy = e.createdBy;
        return e;
    }

    public void rotateRefreshTokenCipher(String newCipher, String byName) {
        this.refreshTokenCipher = newCipher;
        this.updatedBy = byName;
    }
}
