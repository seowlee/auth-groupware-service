package pharos.groupware.service.domain.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

@Controller
@RequestMapping("/leaves")
public class LeaveViewController {
    // 연차 목록
    @GetMapping()
    public String showLeaveListPage(HttpServletRequest request, Model model) {
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());

        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return "leave/leave-list :: content";
        }
        return "account/home";
    }

    // 연차 캘린더
    @GetMapping("/calendar")
    public String vacationCalendar(HttpServletRequest request, Model model) {
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return "leave/leave-calendar :: content";
        }
        return "account/home";
    }

    // 연차 등록 폼
    @GetMapping("/apply")
    public String showCreateLeaveForm(HttpServletRequest request, Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());

        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "leave/create-leave-form :: content";
        }
        return "account/home";
    }

    // 연차 상세 정보
    @GetMapping("/{id}")
    public String showLeaveForm(HttpServletRequest request, Model model, @PathVariable String id) {
        model.addAttribute("mode", "edit");
        model.addAttribute("leaveId", id);
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return "leave/leave-form :: content";
        }
        return "account/home";
    }
}
