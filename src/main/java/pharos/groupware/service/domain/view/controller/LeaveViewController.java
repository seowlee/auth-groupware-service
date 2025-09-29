package pharos.groupware.service.domain.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/leaves")
public class LeaveViewController {
    // 연차 목록
    @GetMapping()
    public String showLeaveListPage(HttpServletRequest request, Model model) {
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        List<LeaveStatusEnum> leaveStatuses = Arrays.stream(LeaveStatusEnum.values())
                .filter(s -> s != LeaveStatusEnum.PENDING) // PENDING 숨김
                .toList();
        // TODO: session
//        request.getSession().setAttribute("userdto", userDto);
        model.addAttribute("leaveStatuses", leaveStatuses);
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
        // 정산용 타입 제외
        List<LeaveTypeEnum> leaveTypes = Arrays.stream(LeaveTypeEnum.values())
                .filter(t -> t != LeaveTypeEnum.ADVANCE && t != LeaveTypeEnum.BORROWED)
                .toList();
        model.addAttribute("leaveTypes", leaveTypes);

        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
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
