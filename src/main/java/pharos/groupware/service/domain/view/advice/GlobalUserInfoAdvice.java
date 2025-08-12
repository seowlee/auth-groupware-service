package pharos.groupware.service.domain.view.advice;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pharos.groupware.service.common.security.CustomUserDetails;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.util.UUID;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalUserInfoAdvice {

    private final UserRepository userRepository;

    @ModelAttribute
    public void addUserInfoToModel(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return;

        Object principal = authentication.getPrincipal();
        User user = null;

        if (principal instanceof CustomUserDetails customUser) {
            user = userRepository.findByUserUuid(customUser.getUserUuid())
                    .orElse(null);
        } else {
            // OIDC
            UUID uuid = UUID.fromString(authentication.getName());
            user = userRepository.findByUserUuid(uuid).orElse(null);
        }


        if (user != null) {
            model.addAttribute("userUuid", user.getUserUuid().toString());
            model.addAttribute("username", user.getUsername());
            model.addAttribute("role", user.getRole().name());
            model.addAttribute("roleDisplayName", user.getRole().getDescription());
            model.addAttribute("isSuperAdmin", user.getRole().isSuperAdmin());
        }
    }
}
