package pharos.groupware.service.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Objects;

@Controller
public class AuthViewController {
    @GetMapping
    public String index() {
        return "OK";
    }

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

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        Object principal = auth.getPrincipal();
        String username = "";
        if (principal instanceof OAuth2User oAuth2User) {
            username = Objects.requireNonNull(oAuth2User.getAttribute("preferred_username")).toString();
        } else if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername(); // local login
        }
        model.addAttribute("username", username);

        // ROLE_USER, ROLE_ADMIN, ROLE_MASTER 중 하나만 전달
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> r.contains("ROLE_"))
                .map(r -> r.replace("ROLE_", ""))
                .findFirst()
                .orElse("USER");

        model.addAttribute("role", role);
        return "home";  // src/main/resources/templates/home.html
    }
}
