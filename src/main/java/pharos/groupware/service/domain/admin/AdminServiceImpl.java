package pharos.groupware.service.domain.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.jwt.JwtTokenProvider;
import pharos.groupware.service.domain.admin.dto.LoginReqDto;
import pharos.groupware.service.domain.admin.dto.LoginResDto;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityContextRepository securityContextRepository;

    @Override
    public LoginResDto login(LoginReqDto reqDto) {
        User user = userRepository.findByUsername(reqDto.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if (!user.getRole().equals(UserRoleEnum.SUPER_ADMIN)) {
            throw new RuntimeException("최고관리자 권한이 필요합니다.");
        }
        if (!passwordEncoder.matches(reqDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 오류");
        }
        // ✅ 2. Spring Security 인증 토큰 생성 (getAuthorities() 문제 해결)
        // Enum의 이름으로 권한을 생성합니다. 'ROLE_' 접두사는 Spring Security의 규칙입니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );

        // ✅ 3. SecurityContext에 인증 정보 등록 -> 이 코드로 인해 세션이 생성됩니다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
//        String accessToken = "accessToken";
//        String refreshToken = "refreshToken";

        return new LoginResDto(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public LoginResDto login(LoginReqDto reqDto, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByUsername(reqDto.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        if (!user.getRole().equals(UserRoleEnum.SUPER_ADMIN)) {
            throw new RuntimeException("최고관리자 권한이 필요합니다.");
        }
        if (!passwordEncoder.matches(reqDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 오류");
        }
        // Spring Security 인증 토큰 생성 (getAuthorities() 문제 해결)
        // Enum의 이름으로 권한을 생성합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserUuid().toString(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );

        // SecurityContext에 인증 정보 등록
        var context = SecurityContextHolder.createEmptyContext(); // 빈 컨텍스트 생성
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        //  생성된 인증 정보를 SecurityContextRepository를 통해 세션에 강제로 저장
        securityContextRepository.saveContext(context, request, response);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        return new LoginResDto(accessToken, refreshToken);
    }
}
