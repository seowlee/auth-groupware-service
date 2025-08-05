package pharos.groupware.service.view.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/leave")
public class LeaveViewController {
    // 연차 목록
    @GetMapping("/list")
    public String showLeaveListPage() {
        return "leave/leave-list";
    }

    // 연차 캘린더
    @GetMapping("/calendar")
    public String vacationCalendar() {
        return "leave/leave-calendar";
    }

    // 연차 등록 폼
    @GetMapping("/apply")
    public String showCreateLeaveForm() {
        return "leave/leave-form";  // src/main/resources/templates/admin/create-user-form.html
    }
}
