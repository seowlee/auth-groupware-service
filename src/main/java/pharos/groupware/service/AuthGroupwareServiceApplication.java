package pharos.groupware.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AuthGroupwareServiceApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(AuthGroupwareServiceApplication.class, args);// ← 로컬/내장톰캣 실행도 그대로 가능
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // ← 외부 Tomcat이 WAR을 로딩할 때 이 메서드가 실행되어 Spring Boot가 부팅됨
        return builder.sources(AuthGroupwareServiceApplication.class);
    }

}
