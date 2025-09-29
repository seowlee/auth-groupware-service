package pharos.groupware.service.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    public ResponseEntity<Void> devtoolsProbe() {
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
