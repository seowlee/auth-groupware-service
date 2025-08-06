package pharos.groupware.service.team.service;

import jakarta.transaction.Transactional;
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
import pharos.groupware.service.admin.dto.PendingUserDto;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.team.domain.Team;
import pharos.groupware.service.team.domain.TeamRepository;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.domain.UserRepository;
import pharos.groupware.service.team.dto.CreateIdpUserReqDto;
import pharos.groupware.service.team.dto.UserDetailResDto;
import pharos.groupware.service.team.dto.UserResDto;
import pharos.groupware.service.team.dto.UserSearchReqDto;

import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long createUser(CreateUserReqDto reqDTO, String currentUsername) {

        Team team = teamRepository.findById(reqDTO.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        User user = User.create(reqDTO, team, currentUsername, passwordEncoder);
        User savedUser = userRepository.save(user);

        return savedUser.getId();
    }

    @Override
    public User createIdpUser(CreateIdpUserReqDto dto) {
        User user = User.create(dto, passwordEncoder);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void registerPendingUser(PendingUserDto reqDto) {
        Team defaultTeam = teamRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("등록된 팀이 없습니다"));
        User user = User.fromPendingDto(reqDto, defaultTeam, passwordEncoder);
        userRepository.save(user);
        log.info("승인 대기 사용자 등록 완료: {}", user.getEmail());
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

        // 정렬 조건 없으면 입사일 내림차순
        if (pageable.getSort().isUnsorted()) {
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

//        System.out.println("page = " + page);
        return page.map(UserResDto::fromEntity);
    }

    @Override
    public UserDetailResDto getUserDetail(UUID uuid) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        UserDetailResDto dto = new UserDetailResDto();
        dto.setUuid(user.getUserUuid());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setJoinedDate(user.getJoinedDate().toString());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setTeamId(user.getTeam().getId());
        dto.setTeamName(user.getTeam().getName());
        // 연차 정보는 제외
        return dto;
//        return null;
    }


}
