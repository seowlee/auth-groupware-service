package pharos.groupware.service.domain.account.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.domain.account.dto.*;
import pharos.groupware.service.domain.team.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    void registerPendingUser(PendingUserReqDto createDto);

    void deleteUser(User user);

    void deactivateUser(User user);

    Page<UserResDto> findAllUsers(UserSearchReqDto userSearchReqDto, Pageable pageable);

    UserDetailResDto getUserDetail(UUID uuid, boolean includeBalances);

    Long createUser(CreateUserReqDto createDto, String currentUsername);

    List<UserApplicantResDto> findAllApplicants(String q);

    User getCurrentUser();

    boolean isCurrentUserSuperAdmin();

    void linkKakaoLocally(User existingUser, String kakaoSub);

    void activate(User user);

    void update(User user, UpdateUserByAdminReqDto reqDto);
}
