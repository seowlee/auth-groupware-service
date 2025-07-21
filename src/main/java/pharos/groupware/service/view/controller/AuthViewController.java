package pharos.groupware.service.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthViewController {
    private final UserRepository userRepository;

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

        String userUUID = auth.getName();  // JWT의 subject 값 = UUID
        User user = userRepository.findByUserUuid(UUID.fromString(userUUID))
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        String role = user.getRole().name();
        String username = user.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("role", role);
        return "home";  // src/main/resources/templates/home.html
    }
}
