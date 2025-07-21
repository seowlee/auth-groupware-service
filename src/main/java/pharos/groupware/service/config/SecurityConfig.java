package pharos.groupware.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final List<String> CLIENTS_TO_EXTRACT = List.of("groupware-app");

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/login",
                                "/css/**",
                                "/oauth2/**",
                                "/test/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/graph/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 폼 로그인 : 실제 사용자 로그인 뷰 제공용
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/home", true)
                )

                // Keycloak 또는 외부 IDP용 OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/login")
                                .defaultSuccessUrl("/home", true)
//                        .userInfoEndpoint(user -> user
//                                .userService(oAuth2UserService)
//                        )
                )
                // JWT 기반 API 인증 (Swagger Authorize에 사용)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
