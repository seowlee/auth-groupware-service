package pharos.groupware.service.domain.account.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.enums.AuditActionEnum;
import pharos.groupware.service.common.enums.AuditStatusEnum;
import pharos.groupware.service.common.enums.FblDecisionEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.common.util.AuditLogUtils;
import pharos.groupware.service.common.util.CommonUtils;
import pharos.groupware.service.domain.account.dto.FblAttemptDto;
import pharos.groupware.service.domain.audit.service.AuditLogService;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.infrastructure.keycloak.KeycloakUserService;

import java.util.Optional;
import java.util.UUID;

import static pharos.groupware.service.common.util.AuditLogUtils.details;

@Service
@RequiredArgsConstructor
public class IdpLinkServiceImpl implements IdpLinkService {
    private final KeycloakUserService keycloakUserService;
    private final AuditLogService auditLogService;
    private final UserService localUserService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void finalizeKakaoLink(UUID keycloakUserId) {
        User user = userRepository.findByUserUuid(keycloakUserId)
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자 없음"));
        try {
            // Keycloak에서 kakao 링크 조회 → sub(userId) 추출
            // 1) Keycloak 브로커에서 실제 kakao sub 확보 (연동 완료된 상태)
            KeycloakUserService.FederatedIdentityLink link = keycloakUserService.findKakaoIdentity(String.valueOf(keycloakUserId))
                    .orElseThrow(() -> new IllegalStateException("Kakao 연동 정보가 없습니다."));

            String kakaoSub = link.userId();
            // 2) 멱등: 동일 사용자에 동일 sub면 no-op 후 성공 로그
            if (kakaoSub.equals(user.getKakaoSub())) {
                auditLogService.saveLog(
                        user.getId(), user.getUsername(),
                        AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.SUCCESS,
                        details("mode", "persistKakaoSub(idempotent)",
                                "keycloakUserId", keycloakUserId,
                                "kakaoSub", kakaoSub,
                                "ip", AuditLogUtils.currentIp())
                );
                return;
            }
            // / 3) 중복(sub) 충돌 검사. 다른 사용자 충돌
            Optional<User> conflict = userRepository.findByKakaoSubAndStatus(kakaoSub, UserStatusEnum.ACTIVE);
            if (conflict.isPresent() && !conflict.get().getUserUuid().equals(user.getUserUuid())) {
                throw new IllegalStateException("이미 다른 계정에 연동된 Kakao 계정입니다.");
            }
            // 4) 저장
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
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 레이스로 유니크 위반 → 동일 사용자 메시지로 매핑
            auditLogService.saveLog(
                    user.getId(), user.getUsername(),
                    AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.FAILED,
                    details("mode", "persistKakaoSub",
                            "keycloakUserId", keycloakUserId,
                            "ip", AuditLogUtils.currentIp(),
                            "error", "unique-violation")
            );
            throw new IllegalStateException("이미 다른 계정에 연동된 Kakao 계정입니다.");
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

    @Override
    public boolean isKakaoLinked(UUID uuid) {
        return userRepository.existsByUserUuidAndKakaoSubIsNotNull(uuid);
    }

    @Override
    @Transactional
    public FblDecisionEnum assessSocialLoginAttempt(FblAttemptDto reqDto) {
        try {
            final String kakaoSub = reqDto.getProviderUserId();
            if (kakaoSub == null || kakaoSub.isBlank()) {
                throw new IllegalArgumentException("Kakao sub가 필요합니다.");
            }
            // 0) kakaoSub가 이미 누군가에게 붙어있나?
            Optional<User> existingBySub = userRepository.findByKakaoSubAndStatus(kakaoSub, UserStatusEnum.ACTIVE);

            // 1) 로컬 사용자 탐색(우선순위: phone → email)
            String normalizedPhone = CommonUtils.phoneNumberNormalize(reqDto.getPhoneNumber());
            Optional<User> byPhone = (normalizedPhone == null || normalizedPhone.isBlank())
                    ? Optional.empty()
                    : userRepository.findByPhoneNumberAndStatus(normalizedPhone, UserStatusEnum.ACTIVE);

            Optional<User> byEmail = (reqDto.getEmail() == null || reqDto.getEmail().isBlank())
                    ? Optional.empty()
                    : userRepository.findByEmailIgnoreCaseAndStatus(reqDto.getEmail(), UserStatusEnum.ACTIVE);

            Optional<User> target = byPhone.isPresent() ? byPhone : byEmail;

            if (existingBySub.isPresent()) {
                // kakaoSub는 이미 시스템에 등록됨
                if (target.isPresent() && !existingBySub.get().getUserUuid().equals(target.get().getUserUuid())) {
                    // 다른 사용자에 붙어있음 → 충돌
                    auditLogService.saveLog(existingBySub.get().getId(), existingBySub.get().getUsername(),
                            AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.FAILED,
                            details("mode", "assess", "result", "link-conflict", "kakaoSub", kakaoSub));
                    return FblDecisionEnum.LINK_CONFLICT;
                } else {
                    // 같은 사용자에 이미 붙어있음 → 멱등
                    auditLogService.saveLog(existingBySub.get().getId(), existingBySub.get().getUsername(),
                            AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.SUCCESS,
                            details("mode", "assess", "result", "already-linked", "kakaoSub", kakaoSub));
                    return FblDecisionEnum.ALREADY_LINKED;
                }
            }

            if (target.isPresent()) {
                // 기존 사용자 매칭 → '연동 필요' 안내
                User user = target.get();

                auditLogService.saveLog(
                        user.getId(), user.getUsername(),
                        AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.SUCCESS,
                        details(
                                "mode", "link-required",                    // [CHANGED] auto-link → link-required
                                "reason", "user_exists_without_kakao_link",
                                "matchBy", byPhone.isPresent() ? "phone" : "email",
                                "kakaoSub", kakaoSub,
                                "phone", normalizedPhone
                        )
                );
                return FblDecisionEnum.LINK_REQUIRED;
            }

            // 2) 완전 신규 → 승인대기 등록
            reqDto.setPhoneNumber(normalizedPhone);
            Long pendingUserId = localUserService.registerPendingUser(reqDto);
            auditLogService.saveLog(
                    null, "anonymous",
                    AuditActionEnum.USER_PENDING, AuditStatusEnum.SUCCESS,
                    details("pendingUserId", pendingUserId, "email", reqDto.getEmail())
            );
            return FblDecisionEnum.PENDING_CREATED;

        } catch (org.springframework.dao.DataIntegrityViolationException unique) {
            // DB UNIQUE(kakao_sub) 최종 보루
            auditLogService.saveLog(
                    null, "anonymous",
                    AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.FAILED,
                    details("error", unique.getMessage(), "email", reqDto.getEmail())
            );
            return FblDecisionEnum.LINK_CONFLICT;
        } catch (Exception e) {
            auditLogService.saveLog(
                    null, "anonymous",
                    AuditActionEnum.USER_SOCIAL_LINK, AuditStatusEnum.FAILED,
                    details("error", e.getMessage(), "email", reqDto.getEmail())
            );
            throw new IllegalStateException("소셜 연동 사용자 처리 중 오류가 발생했습니다.");
        }
    }

}
