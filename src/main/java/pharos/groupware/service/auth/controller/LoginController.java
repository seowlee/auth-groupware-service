package pharos.groupware.service.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OAuth2User user, Model model) {
        // 폼 로그인 사용자라면 Principal 대신 AuthenticationPrincipal UserDetails
        model.addAttribute("username", user.getAttribute("name"));
        return "home";  // src/main/resources/templates/home.html
    }
}
