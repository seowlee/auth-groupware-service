package pharos.groupware.service.domain.view.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;
import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.common.session.SessionKeys;
import pharos.groupware.service.domain.team.dto.TeamDto;
import pharos.groupware.service.domain.team.dto.UserApplicantResDto;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.service.TeamService;
import pharos.groupware.service.domain.team.service.UserService;
import pharos.groupware.service.domain.view.dto.LeaveListFilterViewDto;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/leaves")
public class LeaveViewController {
    private final UserService userService;
    private final TeamService teamService;
    private final ObjectMapper objectMapper;

    // 연차 목록
    @GetMapping()
    public String showLeaveListPage(
            HttpServletRequest request,
            Model model,
            @SessionAttribute(value = SessionKeys.CURRENT_USER, required = false) AppUser actor
    ) throws JsonProcessingException {
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        List<LeaveStatusEnum> leaveStatuses = Arrays.stream(LeaveStatusEnum.values())
                .filter(s -> s != LeaveStatusEnum.PENDING) // PENDING 숨김
                .toList();
        model.addAttribute("leaveStatuses", leaveStatuses);

        User user = (actor != null)
                ? userService.getAuthenticatedUser()
                : null;

        boolean superAdmin = actor != null && actor.role() != null && actor.role().isSuperAdmin();
        boolean teamLeader = actor != null && actor.role() != null && actor.role().isTeamLeader();

        Long teamId = user != null && user.getTeam() != null ? user.getTeam().getId() : null;
        int yearNumber = user != null ? user.getYearNumber() : 1;
        LocalDate joinedDate = user != null ? user.getJoinedDate() : null;

        // 팀 목록 (모델로)
        List<TeamDto> teams = teamService.findAllTeams();
        model.addAttribute("teams", teams);
        // 신청자 목록 (역할별)
        List<UserApplicantResDto> applicants = Collections.emptyList();
        if (superAdmin) {
            applicants = userService.findAllApplicants(null);
        } else if (teamLeader && teamId != null) {
            applicants = userService.findApplicantsByTeam(teamId, null);
        }
        model.addAttribute("applicants", applicants);
        var viewDto = LeaveListFilterViewDto.from(actor, user);
        String viewDtoJson = objectMapper.writeValueAsString(viewDto);

        model.addAttribute("leaveFilterView", viewDto);
        model.addAttribute("leaveFilterViewJson", viewDtoJson);

        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return "leave/leave-list :: content";
        }
        return "forward:/home";
    }

    // 연차 캘린더
    @GetMapping("/calendar")
    public String vacationCalendar(HttpServletRequest request, Model model) {
        model.addAttribute("leaveTypes", LeaveTypeEnum.values());
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr)) {
            return "leave/leave-calendar :: content";
        }
        return "forward:/home";
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
        return "forward:/home";
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
        return "forward:/home";
    }
}
