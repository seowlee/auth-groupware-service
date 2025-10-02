package pharos.groupware.service.domain.view.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        model.addAttribute("roles", UserRoleEnum.values());
        model.addAttribute("statuses", UserStatusEnum.values());
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            // fragment만 내려보냄
            return "team/user-list :: content";
        }
        // 전체 페이지 (최초 진입 시)
        return "forward:/home";

    }

    // 사용자 상세 정보
    @GetMapping("/users/{userId}")
    public String userDetailFragment(HttpServletRequest request, Model model, @PathVariable String userId) throws JsonProcessingException {
//        User user = userRepository.findByUserUuid(UUID.fromString(userId))
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        model.addAttribute("user", user); // Thymeleaf 바인딩용

        model.addAttribute("roles", UserRoleEnum.values());
        model.addAttribute("userStatuses", UserStatusEnum.values());
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        // JS에서 쓸 수 있게 “필요 필드만” JSON으로 가공
        ObjectMapper om = new ObjectMapper();

        List<Map<String, String>> jsRoles = Arrays.stream(UserRoleEnum.values())
                .map(e -> Map.of("name", e.name(), "description", e.getDescription()))
                .toList();

        List<Map<String, String>> jsStatuses = Arrays.stream(UserStatusEnum.values())
                .map(e -> Map.of("name", e.name(), "description", e.getDescription()))
                .toList();

        List<Map<String, Object>> jsLeaveTypes = Arrays.stream(LeaveTypeEnum.values())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.name());
                    m.put("krName", e.getKrName());
                    m.put("parent", e.getParent());   // ← 들여쓰기용
                    // 화면 배지 클래스도 서버에서 내려주면 더 깔끔(필요 없으면 제거)
                    String cls = switch (e) {
                        case ANNUAL -> "lb-type-annual";
                        case BIRTHDAY -> "lb-type-birthday";
                        case SICK -> "lb-type-sick";
                        default -> "lb-type-custom";
                    };
                    m.put("cls", cls);
                    return m;
                })
                .toList();

        Map<String, Object> enumsPayload = Map.of(
                "roles", jsRoles,
                "statuses", jsStatuses,
                "leaveTypes", jsLeaveTypes
        );

        model.addAttribute("enumsJson", om.writeValueAsString(enumsPayload));

        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "team/user-detail :: content";
        }
        // 직접 주소 접근이면 home 전체 + fragment
        return "forward:/home";

    }
}
