//package pharos.groupware.service.common.security;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
//import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import pharos.groupware.service.common.enums.UserStatusEnum;
//import pharos.groupware.service.common.exception.RedirectToPendingApprovalException;
//import pharos.groupware.service.team.domain.User;
//import pharos.groupware.service.team.domain.UserRepository;
//
//import java.util.Optional;
//
//@Slf4j
/// /@Component
//@RequiredArgsConstructor
//public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
//
//    private final UserRepository userRepository;
//
//    //    private final TeamRepository teamRepository;
//    private final PasswordEncoder passwordEncoder;
//

//    @Override
//    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
//        OidcUserService delegate = new OidcUserService();
//        OidcUser oidcUser = delegate.loadUser(userRequest);
//        String email = oidcUser.getEmail();
//        System.out.println("oidc=============" + oidcUser.getUserInfo());
//        System.out.println(oidcUser.getAttributes());
//        // 이미 등록된 사용자라면 통과
//        Optional<User> existingUser = userRepository.findByEmail(email);
//        if (existingUser.isPresent()) {
//            if (existingUser.get().getStatus() != UserStatusEnum.ACTIVE) {
//                throw new OAuth2AuthenticationException(
//                        new OAuth2Error("pending_approval"),
//                        "승인 대기 중 사용자입니다."
//                );
//            }
//            return oidcUser;
//        }
//
//        // 신규 사용자 → 로컬 DB에 PENDING 상태로 등록
//        CreateUserReqDto reqDto = extractUserDto(oidcUser);
/// /        Team defaultTeam = teamRepository.findDefaultTeam().orElse(null); // 기본 팀 또는 null 허용
//        String system = "system";
//
//        User newUser = User.create(reqDto, system, passwordEncoder);
//        userRepository.save(newUser);
//
//        // 사용자 등록은 했지만 승인 안 됨 → 예외로 중단 후 리디렉션
//        throw new OAuth2AuthenticationException(
//                new OAuth2Error("pending_approval", "승인 대기 중 사용자입니다.", "/error/pending-approval")
//        );
//    }
//
//    private CreateUserReqDto extractUserDto(OidcUser user) {
//        Map<String, Object> attr = user.getAttributes();
//        CreateUserReqDto dto = new CreateUserReqDto();
//
//        dto.setEmail(user.getEmail());
//        dto.setUsername((String) attr.getOrDefault("preferred_username", user.getEmail())); // 없으면 email
//        dto.setFirstName((String) attr.get("given_name"));
//        dto.setLastName((String) attr.get("family_name"));
//        dto.setRawPassword("1234"); // 임시 비밀번호
//        dto.setJoinedDate(LocalDate.now());
//        dto.setYearNumber(LocalDate.now().getYear());
//        dto.setRole(UserRoleEnum.TEAM_MEMBER); // 기본 역할 부여
//
//        return dto;
//    }
//}
//
