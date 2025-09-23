package pharos.groupware.service.domain.account.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.common.util.AuditLogUtils;
import pharos.groupware.service.domain.audit.service.AuditLogService;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.util.UUID;

import static pharos.groupware.service.common.util.AuditLogUtils.details;

@Service
@RequiredArgsConstructor
public class IdpLinkServiceImpl implements IdpLinkService {
    private final KeycloakUserService keycloakUserService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void persistKakaoSub(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자 없음"));
        try {
            // Keycloak에서 kakao 링크 조회 → sub(userId) 추출
            KeycloakUserService.FederatedIdentityLink link = keycloakUserService.findKakaoIdentity(keycloakUserId)
                    .orElseThrow(() -> new IllegalStateException("Kakao 연동 정보가 없습니다."));

            String kakaoSub = link.userId();
            // 중복(sub) 충돌 검사
//        Optional<User> conflict = userRepository.findByKakaoSub(kakaoSub);
//        if (conflict.isPresent() && !conflict.get().getUserUuid().equals(user.getUserUuid())) {
//            throw new IllegalStateException("이미 다른 계정에 연동된 Kakao 계정입니다.");
//        }
            user.linkKakao(kakaoSub);
            auditLogService.saveLog(
                    user.getId(),                 // actorId = 본인
                    user.getUsername(),           // actorName
                    AuditActionEnum.USER_SOCIAL_LINK,
                    AuditStatusEnum.SUCCESS,
                    details(
                            "mode", "persistKakaoSub",
                            "keycloakUserId", keycloakUserId,
                            "kakaoSub", kakaoSub,
                            "ip", AuditLogUtils.currentIp()
                    )
            );
        } catch (Exception e) {
            // 실패 감사로그 (actor는 본인)
            auditLogService.saveLog(
                    user.getId(),
                    user.getUsername(),
                    AuditActionEnum.USER_SOCIAL_LINK,
                    AuditStatusEnum.FAILED,
                    details(
                            "mode", "persistKakaoSub",
                            "keycloakUserId", keycloakUserId,
                            "ip", AuditLogUtils.currentIp(),
                            "error", e.getMessage()
                    )
            );
            // 사용자 메시지 정리
            throw new IllegalStateException("카카오 연동에 실패했습니다");
        }
    }
}
