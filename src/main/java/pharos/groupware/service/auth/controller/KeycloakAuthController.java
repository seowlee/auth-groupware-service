package pharos.groupware.service.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.auth.service.KeycloakAuthService;

import java.net.URI;

@RestController
@RequestMapping("/keycloak/auth")
public class KeycloakAuthController {

    private final KeycloakAuthService authService;

    public KeycloakAuthController(KeycloakAuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<String> addKeycloakUser(@RequestBody CreateUserReqDTO reqDTO) {
        String newUserId = authService.createUser(reqDTO);
        URI location = URI.create("/api/users/" + newUserId);
        return ResponseEntity.created(location).body(newUserId);
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('MASTER')")
    public String deleteKeycloakUser(@PathVariable("userId") String userId) {
        authService.deleteUser(userId);
        return "OK";
    }

}
