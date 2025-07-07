package pharos.groupware.service.common;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/user/info")
    @PreAuthorize("hasRole('USER')")
    public String userInfo() {
        return "✅ USER role 접근 성공!";
    }

    @GetMapping("/admin/info")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminInfo() {
        return "✅ ADMIN role 접근 성공!";
    }

    @GetMapping("/public")
    public String publicInfo() {
        return "🌐 누구나 접근 가능한 공개 API";
    }
}
