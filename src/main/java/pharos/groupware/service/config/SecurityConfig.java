package pharos.groupware.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.requestMatchers("/login", "/css/**", "/oauth2/**", "/test/**").permitAll().
                        anyRequest().authenticated()
                )
                // 2) 폼 로그인 (로컬 DB)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/", true)
                )

                // 3) OAuth2 로그인 (Google, Kakao, Keycloak)
                .oauth2Login(oauth2 -> oauth2
                                .loginPage("/login")
//                        .userInfoEndpoint(user -> user
//                                .userService(oAuth2UserService)
//                        )
                )
                .oauth2ResourceServer(rs -> rs
                        .jwt(withDefaults())
                );
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // 우리가 만든 “roles + scope” 변환기를 심어준다
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter());
        return converter;
    }

    // 기존 keycloakRoleConverter() 는 그대로 유지
    private Converter<Jwt, Collection<GrantedAuthority>> keycloakRoleConverter() {
        return jwt -> {
            JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
            scopesConverter.setAuthorityPrefix("SCOPE_");
            Collection<GrantedAuthority> auths = scopesConverter.convert(jwt);

            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null && resourceAccess.containsKey("spring-app")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) ((Map<String, Object>) resourceAccess.get("spring-app")).get("roles");
                auths = Stream.concat(
                        auths.stream(),
                        roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                ).collect(Collectors.toSet());
            }
            return auths;
        };
    }

}
