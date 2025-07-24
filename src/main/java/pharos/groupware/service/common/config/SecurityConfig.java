package pharos.groupware.service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final List<String> CLIENTS_TO_EXTRACT = List.of("groupware-app");

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/login",
                                "/admin/login",
                                "/css/**",
                                "/oauth2/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 폼 로그인 : 실제 사용자 로그인 뷰 제공용
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/home", true)
                        .failureHandler((request, response, exception) -> {
                            String errorMessage = "로그인 실패";

                            Throwable cause = exception.getCause();

                            if (exception instanceof BadCredentialsException) {
                                errorMessage = "아이디 또는 비밀번호가 잘못되었습니다.";
                            } else if (exception instanceof DisabledException || cause instanceof DisabledException) {
                                errorMessage = "계정이 비활성화되었습니다.";
                            } else if (exception instanceof UsernameNotFoundException || cause instanceof UsernameNotFoundException) {
                                errorMessage = "사용자를 찾을 수 없습니다.";
                            } else if (cause instanceof InsufficientAuthenticationException) {
                                errorMessage = "최고관리자만 로그인할 수 있습니다.";
                            }

                            request.getSession().setAttribute("errorMessage", errorMessage);
                            response.sendRedirect("/login?error");
                        })
                )
                // Keycloak 또는 외부 IDP용 OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/login")
                                .defaultSuccessUrl("/home", true)
//                        .userInfoEndpoint(user -> user
//                                .userService(oAuth2UserService)
//                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
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

    @Bean
    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}/login");
        return successHandler;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

}
