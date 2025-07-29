package pharos.groupware.service.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.team.domain.Team;
import pharos.groupware.service.team.domain.TeamRepository;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;
import pharos.groupware.service.team.dto.CreateIdpUserReqDto;
import pharos.groupware.service.team.dto.UserResDto;
import pharos.groupware.service.team.dto.UserSearchReqDto;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String createUser(CreateUserReqDto reqDTO) {

        Team team = teamRepository.findById(reqDTO.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        User user = User.create(reqDTO, team, passwordEncoder);
        User savedUser = userRepository.save(user);

        return savedUser.getUserUuid().toString();
    }

    @Override
    public User createIdpUser(CreateIdpUserReqDto dto) {
        User user = User.create(dto, passwordEncoder);
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(User user) {
        userRepository.deleteByUserUuid(user.getUserUuid());
    }

    @Override
    public void deactivateUser(User user) {
        user.deactivate();
    }

    @Override
    public Page<UserResDto> findAllUsers(UserSearchReqDto reqDto, Pageable pageable) {
//        Specification<User> spec = UserSpecification.search(reqDto);
//        Page<User> page = userRepository.findAll(spec, pageable);
        // 1. 클라이언트가 정렬 조건을 보냈는지 확인합니다.
        if (pageable.getSort().isUnsorted()) {
            // 2. 정렬 조건이 없다면, 안정적인 기본 정렬을 새로 만들어 적용합니다.
            Sort defaultSort = Sort.by(Sort.Order.desc("joinedDate"), Sort.Order.asc("id"));
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }

        UserRoleEnum roleEnum = null;
        String roleStr = reqDto.getRole();
        if (StringUtils.hasText(roleStr)) {
            roleEnum = UserRoleEnum.valueOf(roleStr.toUpperCase());
        }

        UserStatusEnum statusEnum = null;
        String statusStr = reqDto.getStatus();
        if (StringUtils.hasText(statusStr)) {
            statusEnum = UserStatusEnum.valueOf(statusStr.toUpperCase());
        }
  
        Page<User> page = userRepository.findAllBySearchFilter(reqDto.getKeyword(), reqDto.getTeamId(), roleEnum, statusEnum, pageable);

        System.out.println("page = " + page);
        return page.map(UserResDto::fromEntity);
    }
}
