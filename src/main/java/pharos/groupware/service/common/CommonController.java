package pharos.groupware.service.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonController {
    @GetMapping("/")
    public String health() {
        return "OK";
    }
}
