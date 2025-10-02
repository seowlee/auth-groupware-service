package pharos.groupware.service.domain.account.service;

import pharos.groupware.service.common.enums.FblDecisionEnum;
import pharos.groupware.service.domain.account.dto.FblAttemptDto;

import java.util.UUID;

public interface IdpLinkService {
    void finalizeKakaoLink(UUID keycloakUserId);

    boolean isKakaoLinked(UUID uuid);

    FblDecisionEnum assessSocialLoginAttempt(FblAttemptDto reqDto);
}
