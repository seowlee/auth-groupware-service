package pharos.groupware.service.team.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.admin.dto.PendingUserDto;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.dto.CreateIdpUserReqDto;
import pharos.groupware.service.team.dto.UserDetailResDto;
import pharos.groupware.service.team.dto.UserResDto;
import pharos.groupware.service.team.dto.UserSearchReqDto;

import java.util.UUID;

public interface UserService {

    User createIdpUser(CreateIdpUserReqDto dto);

    void registerPendingUser(PendingUserDto createDto);

    void deleteUser(User user);

    void deactivateUser(User user);

    Page<UserResDto> findAllUsers(UserSearchReqDto userSearchReqDto, Pageable pageable);

    UserDetailResDto getUserDetail(UUID uuid);

    Long createUser(CreateUserReqDto createDto, String currentUsername);
}
