package pharos.groupware.service.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pharos.groupware.service.common.security.LoginSuccessHandler;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final LoginSuccessHandler loginSuccessHandler;

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // [ADD] 경로 바인딩 (deprecated 없는 최신식)
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트(OPTIONS) 무조건 통과
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // Keycloak FTL에서 호출하는 공개 엔드포인트 허용
                        .requestMatchers("/api/idp/fbl/decision").permitAll()
                        // (필요 시 health, docs 등 추가)
                        // .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 401
                        .accessDeniedHandler((req, res, ex) -> { // 403
                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"message\":\"권한이 없습니다.\"}");
                        })
                )
                // JWT 기반 API 인증 (Swagger Authorize에 사용)
                // Swagger용 JWT(Resource Server)는 세션에 의존하지 않음
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/.well-known/**", "/favicon.ico").permitAll()
                        .requestMatchers("/health", "/login", "/api/admin/login", "/oauth2/**", "/link/kakao/callback", "/admin/m365/**").permitAll()
                        .requestMatchers("/realms/**", "/error/pending-approval").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .requestCache(c -> c.requestCache(requestCache()))
                .exceptionHandling(e -> e
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")
                        )
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
                        .successHandler(loginSuccessHandler)
                )
                // Keycloak 또는 외부 IDP용 OAuth2 로그인
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
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

    @Bean
    RequestCache requestCache() {
        return new HttpSessionRequestCache() {
            @Override
            public void saveRequest(HttpServletRequest req, HttpServletResponse res) {
                String uri = req.getRequestURI();
                if ("/".equals(uri)
                        || uri.equals("/favicon.ico")
                        || uri.startsWith("/.well-known/")
                        || uri.startsWith("/admin/m365/")
                        || uri.startsWith("/link/kakao/")) {
                    return; // 저장 안 함
                }
                super.saveRequest(req, res);
            }
        };
    }

    // ===  Security CORS Bean: 여기 하나로만 관리 ===
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 개발: Keycloak 오리진만 허용 (필요 시 추가)
        cfg.setAllowedOrigins(List.of("http://localhost:8081"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(false); // fetch: credentials: "omit"
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 전체 또는 "/api/**"로 좁혀도 됨
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
