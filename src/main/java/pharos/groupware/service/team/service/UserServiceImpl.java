package pharos.groupware.service.team.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;

import java.util.UUID;


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
        User user = User.create(reqDTO, passwordEncoder);
        User savedUser = userRepository.save(user);

        return savedUser.getUserUuid().toString();
    }

    @Override
    public void deleteUser(UUID uuid) {
        userRepository.deleteByUserUuid(uuid);
    }

    @Override
    public void deactivateUser(UUID uuid) {
        User user = userRepository.findByUserUuid(uuid).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        user.deactivate();
    }
}
