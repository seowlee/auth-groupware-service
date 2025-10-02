package pharos.groupware.service.domain.view.advice;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.common.session.SessionKeys;
import pharos.groupware.service.domain.team.entity.UserRepository;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalUserInfoAdvice {

    private final UserRepository userRepository;

    @ModelAttribute
    public void addUserInfoToModel(
            @SessionAttribute(value = SessionKeys.CURRENT_USER, required = false) AppUser actor,
            Authentication authentication, Model model) {
        if (actor == null) return;
        model.addAttribute("userUuid", actor.userUuid() != null ? actor.userUuid().toString() : null);
        model.addAttribute("username", actor.username());
        model.addAttribute("role", actor.role().name());
        model.addAttribute("roleDisplayName", actor.role().getDescription());
        model.addAttribute("isSuperAdmin", actor.role().isSuperAdmin());

//        if (authentication == null || !authentication.isAuthenticated()) return;
//
//        Object principal = authentication.getPrincipal();
//        User user = null;
//
//        if (principal instanceof CustomUserDetails customUser) {
//            user = userRepository.findByUserUuid(customUser.getUserUuid())
//                    .orElse(null);
//        } else {
//            // OIDC
//            UUID uuid = UUID.fromString(authentication.getName());
//            user = userRepository.findByUserUuid(uuid).orElse(null);
//        }
//
//
//        if (user != null) {
//            model.addAttribute("userUuid", user.getUserUuid().toString());
//            model.addAttribute("username", user.getUsername());
//            model.addAttribute("role", user.getRole().name());
//            model.addAttribute("roleDisplayName", user.getRole().getDescription());
//            model.addAttribute("isSuperAdmin", user.getRole().isSuperAdmin());
//        }
    }
}
