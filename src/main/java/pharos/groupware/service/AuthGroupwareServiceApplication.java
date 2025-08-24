package pharos.groupware.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AuthGroupwareServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthGroupwareServiceApplication.class, args);
    }

}
