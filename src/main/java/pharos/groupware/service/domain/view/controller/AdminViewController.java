package pharos.groupware.service.domain.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    // 사용자 등록 폼
    @GetMapping("/users/new")
    public String showCreateUserForm(HttpServletRequest request) {
//        String xhr = ;
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "admin/create-user-form :: content";  // src/main/resources/templates/admin/create-user-form.html
        }
        return "account/home";
    }

    // 로그 목록 페이지
    @GetMapping("/logs")
    public String showLogListPage() {
        return "admin/audit-log";
    }


}
