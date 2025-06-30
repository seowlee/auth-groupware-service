package pharos.groupware.service.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pharos.groupware.service.auth.dto.CreateUserReqDTO;
import pharos.groupware.service.user.domain.User;
import pharos.groupware.service.user.domain.UserRepository;


@Slf4j
@Service
public class LocalAuthServiceImpl implements LocalAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String createUser(CreateUserReqDTO reqDTO) {
        User user = User.from(reqDTO, passwordEncoder);
        User savedUser = userRepository.save(user);
        log.info("로컬 사용자 생성 완료: {}", savedUser.getUsername());
        return savedUser.getUserId().toString();
    }

    @Override
    public void deleteUser(String userId) {

    }
}
