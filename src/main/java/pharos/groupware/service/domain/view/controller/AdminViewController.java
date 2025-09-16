package pharos.groupware.service.domain.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pharos.groupware.service.common.enums.UserRoleEnum;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    // 사용자 등록 폼
    @GetMapping("/users/new")
    public String showCreateUserForm(HttpServletRequest request, Model model) {
        model.addAttribute("roles", UserRoleEnum.values());
//        String xhr = ;
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "admin/create-user-form :: content";  // src/main/resources/templates/admin/create-user-form.html
        }
        return "account/home";
    }

    // 로그 목록 페이지
    @GetMapping("/logs")
    public String showLogListPage(HttpServletRequest request, Model model) {
        model.addAttribute("auditActions", pharos.groupware.service.common.enums.AuditActionEnum.values());
        model.addAttribute("auditStatuses", pharos.groupware.service.common.enums.AuditStatusEnum.values());

        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return "admin/audit-log :: content";
        }
        return "account/home"; // 홈 레이아웃 안에 fragment 삽입

    }
}
