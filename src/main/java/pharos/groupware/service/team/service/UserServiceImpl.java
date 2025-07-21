package pharos.groupware.service.team.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;


@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String createUser(CreateUserReqDto reqDTO) {
        // local db
        User user = User.from(reqDTO, passwordEncoder);
        User savedUser = userRepository.save(user);
        log.info("로컬 사용자 생성 완료: {}", savedUser.getUsername());

        return savedUser.getUserUuid().toString();
    }

    @Override
    public void deleteUser(String userId) {

    }
}
