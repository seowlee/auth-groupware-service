package pharos.groupware.service.view.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    // 사용자 등록 폼
    @GetMapping("/users/create")
    public String showCreateUserForm() {
//        model.addAttribute("userDto", new CreateUserReqDto());
        return "admin/create-user-popup";  // src/main/resources/templates/admin/create-user-popup.html
    }

    // 로그 목록 페이지
    @GetMapping("/logs")
    public String showLogListPage() {
        return "admin/audit-log-list";
    }


}
