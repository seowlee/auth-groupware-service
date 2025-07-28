package pharos.groupware.service.view.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/team")
public class TeamViewController {

    // 사용자 목록
    @GetMapping("/users")
    public String showUsers() {
        return "team/user-list";
    }
    
}
