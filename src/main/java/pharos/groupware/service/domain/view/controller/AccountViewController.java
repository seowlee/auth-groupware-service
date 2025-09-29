package pharos.groupware.service.domain.view.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.common.session.SessionKeys;
import pharos.groupware.service.domain.m365.service.M365IntegrationReadService;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.service.UserService;

@Controller
@RequiredArgsConstructor
public class AccountViewController {
    private final UserService userService;
    private final M365IntegrationReadService m365IntegrationReadService;

    // 1) 루트: 로그인 여부에 따라 리다이렉트
    @GetMapping("/")
    public String root(Authentication auth) {
        boolean loggedIn = (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken));
        return "redirect:" + (loggedIn ? "/home" : "/login");
    }

    // 2) 로그인 페이지: 이미 로그인이면 홈으로
    @GetMapping("/login")
    public String login(Authentication auth) {
        boolean loggedIn = (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken));
        if (loggedIn) {
            return "redirect:/home";
        }
        return "account/login";
    }

    // 3) 홈 페이지: 세션의 AppUser를 (있으면) 모델에 싣기
    @GetMapping("/home")
    public String home(
            @SessionAttribute(value = SessionKeys.CURRENT_USER, required = false) AppUser actor,
            Model model
    ) {
        if (actor != null) {
            model.addAttribute("actor", actor);
            User user = userService.getAuthenticatedUser();
            boolean isSuperAdmin =
                    actor.role() != null && actor.role().isSuperAdmin(); // 프로젝트에 맞게 판별
            boolean kakaoLinked = user.getKakaoSub() != null;                // 필드명 프로젝트에 맞게
            boolean m365Linked = isSuperAdmin && m365IntegrationReadService.isLinked();

            // ↑ 조직 전체/테넌트 기준의 연결 여부를 읽는 서비스 (임의 예시)

            model.addAttribute("isSuperAdmin", isSuperAdmin);
            model.addAttribute("kakaoLinked", kakaoLinked);
            model.addAttribute("m365Linked", m365Linked);
            // ================
        } else {
            model.addAttribute("isSuperAdmin", false);
            model.addAttribute("kakaoLinked", false);
            model.addAttribute("m365Linked", false);
        }
        return "account/home";
    }

    //    @GetMapping("/home")
//    public String home(Authentication authentication, Model model) {
//        Object principal = authentication.getPrincipal();
//        User user;
//        // OIDC 로그인 사용자는 JwtAuthenticationToken이므로 타입 분기 필요
//        if (principal instanceof CustomUserDetails customUser) {
//            UUID userUuid = customUser.getUserUuid();
//            user = userRepository.findByUserUuid(userUuid)
//                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
//
//
//        } else {
//            // OIDC 사용자는 JwtAuthenticationToken 처리 (예시)
//            String sub = authentication.getName();
//            UUID userUuid = UUID.fromString(sub);
//            user = userRepository.findByUserUuid(userUuid)
//                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
//        }
//        model.addAttribute("username", user.getUsername());
//        model.addAttribute("role", user.getRole().name());
//        model.addAttribute("roleDisplayName", user.getRole().getDescription());
//        ;
//        boolean isSuperAdmin = user.getRole().isSuperAdmin();
//        model.addAttribute("isSuperAdmin", isSuperAdmin);
//        return "account/home";
//
//    }
//
    @GetMapping("/error/pending-approval")
    public String pendingApproval(Model model, @RequestParam(name = "email", required = false, defaultValue = "—") String email) {
        model.addAttribute("email", email);
        return "account/pending-approval";
    }

//    @GetMapping("/home")
//    public String home(Authentication auth, Model model) {
//        String rawPrincipal = auth.getName();  // UUID or 일반 로그인 ID
//        System.out.println("🔐 auth.getName(): " + rawPrincipal);
//
//        User user;
//
//        try {
//            // OIDC 로그인 사용자는 UUID 기반으로 조회
//            UUID uuid = UUID.fromString(rawPrincipal);
//            user = userRepository.findByUserUuid(uuid)
//                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
//        } catch (IllegalArgumentException e) {
//            // 폼 로그인 사용자 (예: superadmin)
//            user = userRepository.findByUsername(rawPrincipal)
//                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
//        }
//
//        String role = user.getRole().name();
//        String username = user.getUsername();
//        model.addAttribute("username", username);
//        model.addAttribute("role", role);
//        return "home";  // templates/home.html
//    }

}
