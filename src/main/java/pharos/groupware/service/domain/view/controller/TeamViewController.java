package pharos.groupware.service.domain.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/team")
public class TeamViewController {
//    private final UserRepository userRepository;
//
//    public TeamViewController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }

    // 사용자 목록
    @GetMapping("/users")
    public String showUsers(HttpServletRequest request, Model model) {
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            // fragment만 내려보냄
            return "team/user-list :: content";
        }
        // 전체 페이지 (최초 진입 시)
        return "account/home";
    }

    // 사용자 상세 정보
    @GetMapping("/users/{userId}")
    public String userDetailFragment(HttpServletRequest request, Model model, @PathVariable String userId) {
//        User user = userRepository.findByUserUuid(UUID.fromString(userId))
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        model.addAttribute("user", user); // Thymeleaf 바인딩용
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "team/user-detail :: content";
        }
        // 직접 주소 접근이면 home 전체 + fragment
        return "account/home";
    }
}
