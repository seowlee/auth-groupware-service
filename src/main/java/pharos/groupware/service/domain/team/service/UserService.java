package pharos.groupware.service.domain.team.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.domain.admin.dto.CreateUserReqDto;
import pharos.groupware.service.domain.admin.dto.PendingUserDto;
import pharos.groupware.service.domain.admin.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.team.dto.*;
import pharos.groupware.service.domain.team.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User createIdpUser(CreateIdpUserReqDto dto);

    void registerPendingUser(PendingUserDto createDto);

    void deleteUser(User user);

    void deactivateUser(User user);

    Page<UserResDto> findAllUsers(UserSearchReqDto userSearchReqDto, Pageable pageable);

    UserDetailResDto getUserDetail(UUID uuid);

    Long createUser(CreateUserReqDto createDto, String currentUsername);

    List<UserApplicantResDto> findAllApplicants(String q);

    User getCurrentUser();

    boolean isCurrentUserSuperAdmin();

    void linkKakaoLocally(User existingUser, String kakaoSub);

    void activate(User user);

    void update(User user, UpdateUserByAdminReqDto reqDto);
}
