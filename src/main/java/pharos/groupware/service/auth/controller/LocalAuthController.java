package pharos.groupware.service.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.auth.service.LocalAuthService;

@RestController
@RequestMapping("/local/auth")
public class LocalAuthController {
    private final LocalAuthService localAuthService;

    public LocalAuthController(LocalAuthService localAuthService) {
        this.localAuthService = localAuthService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody CreateUserReqDTO reqDTO) {
        String newUserId = localAuthService.createUser(reqDTO);
        return ResponseEntity.ok(newUserId);
    }
}
