package pharos.groupware.service.view.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pharos.groupware.service.admin.dto.CreateUserReqDto;

@Controller
@RequestMapping("/admin")
public class AdminViewController {
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("userDto", new CreateUserReqDto());
        return "admin/create-user-form";  // src/main/resources/templates/admin/create-user-form.html
    }
}
