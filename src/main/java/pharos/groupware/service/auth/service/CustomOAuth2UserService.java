package pharos.groupware.service.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
//    private final UserRepository userRepo;  // 로컬 DB
//
//    public CustomOAuth2UserService(UserRepository userRepo) {
//        this.userRepo = userRepo;
//    }
//
//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest req)
//            throws OAuth2AuthenticationException {
//        OAuth2User user = super.loadUser(req);
//
//        // 1) 자료 매핑: registrationId에 따라 구글/카카오/Keycloak 속성 꺼내기
//        Map<String,Object> attrs = user.getAttributes();
//        String registrationId = req.getClientRegistration().getRegistrationId();
//        String username = /* attrs → 공통 username 추출 */;
//        String email    = /* attrs → 이메일 추출 */;
//
//        // 2) 로컬 DB에 처음이면 삽입, 아니면 업데이트
//        UserEntity u = userRepo.findByUsername(username)
//                .orElseGet(() -> new UserEntity(username, passwordEncoder().encode(UUID.randomUUID().toString())));
//        u.setEmail(email);
//        u.setProvider(registrationId);
//        userRepo.save(u);
//
//        // 3) Spring Security용 OAuth2User 리턴
//        return new DefaultOAuth2User(
//                List.of(new SimpleGrantedAuthority("ROLE_USER")),
//                attrs, "sub"  // 또는 registrationId마다 id키 지정
//        );
//    }
}
