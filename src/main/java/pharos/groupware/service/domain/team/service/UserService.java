package pharos.groupware.service.domain.team.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.PendingUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.team.dto.UserDetailResDto;
import pharos.groupware.service.domain.team.dto.UserResDto;
import pharos.groupware.service.domain.team.dto.UserSearchReqDto;
import pharos.groupware.service.domain.team.entity.User;

import java.util.UUID;

public interface UserService {

    Long registerPendingUser(PendingUserReqDto createDto);

    void deleteUser(User user);

    void deactivateUser(User user);

    Page<UserResDto> findAllUsers(UserSearchReqDto userSearchReqDto, Pageable pageable);

    UserDetailResDto getUserDetail(UUID uuid, boolean includeBalances);

    Long createUser(CreateUserReqDto createDto, String currentUsername);


    User getAuthenticatedUser();

    boolean isCurrentUserSuperAdmin();

    void linkKakaoLocally(User existingUser, String kakaoSub);

    void activate(User user);

    void update(User user, UpdateUserByAdminReqDto reqDto);
}
