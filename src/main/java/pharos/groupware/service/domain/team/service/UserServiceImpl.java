package pharos.groupware.service.domain.team.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.common.util.AuthUtils;
import pharos.groupware.service.common.util.LeaveUtils;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.FblAttemptDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.leave.entity.LeaveBalance;
import pharos.groupware.service.domain.leave.entity.LeaveBalanceRepository;
import pharos.groupware.service.domain.team.dto.UserApplicantResDto;
import pharos.groupware.service.domain.team.dto.UserDetailResDto;
import pharos.groupware.service.domain.team.dto.UserResDto;
import pharos.groupware.service.domain.team.dto.UserSearchReqDto;
import pharos.groupware.service.domain.team.entity.Team;
import pharos.groupware.service.domain.team.entity.TeamRepository;
import pharos.groupware.service.domain.team.entity.User;
import pharos.groupware.service.domain.team.entity.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    private static List<UserDetailResDto.LeaveBalanceDto> mapToDtos(List<LeaveBalance> balances, Integer yearNumber) {
        return balances.stream().map(lb -> {
            UserDetailResDto.LeaveBalanceDto dto = new UserDetailResDto.LeaveBalanceDto();
            // enum이면 name(), 문자열이면 그대로
            dto.setLeaveType(lb.getLeaveType().toString());

            BigDecimal total = Optional.ofNullable(lb.getTotalAllocated()).orElse(BigDecimal.ZERO);
            BigDecimal used = Optional.ofNullable(lb.getUsed()).orElse(BigDecimal.ZERO);
//            BigDecimal remain = total.subtract(used);
//            if (remain.signum() < 0) remain = BigDecimal.ZERO;

            dto.setTotalAllocated(total);
            dto.setUsed(used);
            dto.setYearNumber(yearNumber);
            return dto;
        }).toList();
    }

    @Override
    public Long createUser(CreateUserReqDto reqDTO, String currentUsername) {

        teamRepository.findById(reqDTO.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        User user = User.create(reqDTO, currentUsername, passwordEncoder);
        User savedUser = userRepository.save(user);

        return savedUser.getId();
    }

    @Override
    @Transactional
    public Long registerPendingUser(FblAttemptDto reqDto) {
        Team defaultTeam = teamRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("등록된 팀이 없습니다"));
        User user = User.fromPendingDto(reqDto, defaultTeam, passwordEncoder);
        User savedUser = userRepository.save(user);
        log.info("승인 대기 사용자 등록 완료: {}", user.getEmail());
        return savedUser.getId();
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
    public void activate(User user) {
        user.activate();
    }

    @Override
    public void update(User user, UpdateUserByAdminReqDto reqDto) {
        String actor = AuthUtils.getCurrentUsername();
        if (reqDto.getJoinedDate() != null) {
            reqDto.setYearNumber(LeaveUtils.getCurrentYearNumber(reqDto.getJoinedDate()));
        } else {
            reqDto.setYearNumber(user.getYearNumber()); // 유지
        }
        user.updateByAdmin(reqDto, actor);
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
    public UserDetailResDto getUserDetail(UUID uuid, boolean includeBalances, Integer yearNumber) {
        User user = userRepository.findByUserUuid(uuid)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        UserDetailResDto userDetailResDto = UserDetailResDto.fromEntity(user);

        if (includeBalances) {
            Integer yn = (yearNumber != null ? yearNumber : user.getYearNumber());
            List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYearNumber(user.getId(), yn);
            userDetailResDto.setLeaveBalances(mapToDtos(balances, yn));
        }
        return userDetailResDto;
    }

    @Override
    public User getAuthenticatedUser() {
        String uuid = AuthUtils.extractUserUUID();
        return userRepository.findByUserUuid(UUID.fromString(uuid))
                .orElseThrow(() -> new EntityNotFoundException("현재 사용자를 찾을 수 없습니다."));
    }

    @Override
    public boolean isCurrentUserSuperAdmin() {
        return getAuthenticatedUser().getRole().isSuperAdmin();
    }

    @Override
    public boolean isCurrentUserTeamLead() {
        return getAuthenticatedUser().getRole().isTeamLeader();

    }

    @Override
    @Transactional
    public void linkKakaoLocally(User user, String kakaoSub) {
        // sub 충돌 검사
        Optional<User> conflict = userRepository.findByKakaoSubAndStatus(kakaoSub, UserStatusEnum.ACTIVE);
        if (conflict.isPresent() && !conflict.get().getUserUuid().equals(user.getUserUuid())) {
            throw new IllegalStateException("이미 다른 계정에 연동된 Kakao 계정입니다.");
        }
        user.linkKakao(kakaoSub);
    }

    @Override
    public List<UserApplicantResDto> findAllApplicants(String q) {
        return userRepository.findActiveUsersForSelect(q)
                .stream()
                .map(UserApplicantResDto::toApplicantDto)
                .toList();
    }

    public List<UserApplicantResDto> findApplicantsByTeam(Long teamId, String q) {
        return userRepository.findActiveUsersForSelectByTeam(teamId, q).stream()
                .map(UserApplicantResDto::toApplicantDto)
                .toList();
    }
}
