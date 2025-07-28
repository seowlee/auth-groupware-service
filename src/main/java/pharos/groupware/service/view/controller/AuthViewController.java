package pharos.groupware.service.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pharos.groupware.service.common.security.CustomUserDetails;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthViewController {
    private final UserRepository userRepository;


    @GetMapping("/login")
    public String login(HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        // 인증된 사용자는 홈화면으로.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            response.sendRedirect("/home");
            return null;
        }
        return "auth/login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        Object principal = authentication.getPrincipal();
        User user;
        // OIDC 로그인 사용자는 JwtAuthenticationToken이므로 타입 분기 필요
        if (principal instanceof CustomUserDetails customUser) {
            UUID userUuid = customUser.getUserUuid();
            user = userRepository.findByUserUuid(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));


        } else {
            // OIDC 사용자는 JwtAuthenticationToken 처리 (예시)
            String sub = authentication.getName();
            UUID userUuid = UUID.fromString(sub);
            user = userRepository.findByUserUuid(userUuid)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole().name());

        return switch (user.getRole()) {
            case SUPER_ADMIN -> "admin/home";
            case TEAM_LEADER -> "home";
            default -> "home";
        };
    }

    @GetMapping("/error/pending-approval")
    public String pendingApproval(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user != null) {
            model.addAttribute("email", user.getAttribute("email"));
        }
        return "auth/pending-approval";
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
