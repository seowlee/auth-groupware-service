package pharos.groupware.service.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.domain.team.dto.CreateIdpUserReqDto;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;
import pharos.groupware.service.domain.team.service.UserService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final UserService userService;

    public CustomOAuth2SuccessHandler(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2User attributes: {}", oAuth2User.getAttributes());
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 최초 로그인 시 사용자 등록 + 비활성화
                    CreateIdpUserReqDto dto = new CreateIdpUserReqDto();
                    dto.setUserUUID(oAuth2User.getAttribute("sub"));
                    dto.setUsername(oAuth2User.getAttribute("preferred_username"));
                    dto.setEmail(email);
                    dto.setFirstName(oAuth2User.getAttribute("given_name")); // 없으면 null
                    dto.setLastName(oAuth2User.getAttribute("family_name")); // 없으면 null
                    dto.setRawPassword(UUID.randomUUID().toString()); // 카카오 사용자 비밀번호는 랜덤 임시값
                    dto.setJoinedDate(LocalDate.now());
                    dto.setRole(UserRoleEnum.TEAM_MEMBER);
                    dto.setStatus(UserStatusEnum.PENDING);

                    return userService.createIdpUser(dto);
                });

        if (user.getStatus() == UserStatusEnum.PENDING) {

            response.sendRedirect("/error/pending-approval");
            return;
        }

        response.sendRedirect("/home");
    }

}
