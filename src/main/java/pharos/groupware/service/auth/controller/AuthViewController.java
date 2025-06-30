package pharos.groupware.service.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class AuthViewController {

    @GetMapping("/login")
    public String login(HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        // 인증된 사용자는 홈화면으로.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            response.sendRedirect("/");
            return null;
        }
        return "login";
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OAuth2User user, Model model) {
        // 폼 로그인 사용자라면 Principal 대신 AuthenticationPrincipal UserDetails
        model.addAttribute("username", user.getAttribute("name"));
        return "home";  // src/main/resources/templates/home.html
    }
}
