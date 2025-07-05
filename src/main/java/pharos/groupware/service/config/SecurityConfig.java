package pharos.groupware.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final List<String> CLIENTS_TO_EXTRACT = List.of("groupware-app", "groupware-provisioner");

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/login", "/css/**", "/oauth2/**", "/test/**", "/local/auth/**", "/graph/**").permitAll().
                        anyRequest().authenticated()
                )
                // 2) 폼 로그인
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/home", true)
                )

                // 3) OAuth2 로그인 (Google, Kakao, Keycloak)
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/login")
                                .defaultSuccessUrl("/home", true)
//                        .userInfoEndpoint(user -> user
//                                .userService(oAuth2UserService)
//                        )
                )
                .oauth2ResourceServer(rs -> rs
                                .jwt(jwt -> jwt
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                                )
//                        .jwt(withDefaults())
                );
        return http.build();
    }


    // 기존 keycloakRoleConverter() 는 그대로 유지
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // (a) 기본 scope -> SCOPE_ 변환기
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("SCOPE_");

        // (b) Keycloak resource_access.my-client.roles -> ROLE_ 변환기
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // (1) SCOPE_ 변환
            Collection<GrantedAuthority> auths = scopesConverter.convert(jwt);

            // (2) resource_access 내 client 역할 변환
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                auths = Stream.concat(
                        auths.stream(),
                        extractClientRoles(resourceAccess, CLIENTS_TO_EXTRACT).stream()
                ).collect(Collectors.toSet());
            }

            return auths;
        });

        return converter;
    }

    private Collection<GrantedAuthority> extractClientRoles(Map<String, Object> resourceAccess, List<String> clients) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (String client : clients) {
            Object rawClientAccess = resourceAccess.get(client);
            if (rawClientAccess instanceof Map<?, ?> clientAccessRaw) {
                Object rolesRaw = ((Map<?, ?>) clientAccessRaw).get("roles");
                if (rolesRaw instanceof List<?> rolesList) {
                    for (Object role : rolesList) {
                        if (role instanceof String r) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        }
                    }
                }
            }
        }

        return authorities;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
