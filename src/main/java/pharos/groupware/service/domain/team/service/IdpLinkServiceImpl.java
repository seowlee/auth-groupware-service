package pharos.groupware.service.domain.team.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdpLinkServiceImpl implements IdpLinkService {
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void persistKakaoSub(String keycloakUserId) {
        UUID uuid = UUID.fromString(keycloakUserId);
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자 없음"));
        // Keycloak에서 kakao 링크 조회 → sub(userId) 추출
        Optional<KeycloakUserService.FederatedIdentityLink> linkOpt =
                keycloakUserService.findKakaoIdentity(keycloakUserId);
        KeycloakUserService.FederatedIdentityLink link = linkOpt
                .orElseThrow(() -> new IllegalStateException("Kakao 연동 정보가 없습니다."));

        String kakaoSub = link.userId();
        // 중복(sub) 충돌 검사
//        Optional<User> conflict = userRepository.findByKakaoSub(kakaoSub);
//        if (conflict.isPresent() && !conflict.get().getUserUuid().equals(user.getUserUuid())) {
//            throw new IllegalStateException("이미 다른 계정에 연동된 Kakao 계정입니다.");
//        }
        user.linkKakao(kakaoSub);
    }
}
