package pharos.groupware.service.team.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.team.domain.User;
import pharos.groupware.service.team.dto.CreateIdpUserReqDto;
import pharos.groupware.service.team.dto.UserResDto;
import pharos.groupware.service.team.dto.UserSearchReqDto;

public interface UserService {
    String createUser(CreateUserReqDto reqDTO);

    User createIdpUser(CreateIdpUserReqDto dto);

    void deleteUser(User user);

    void deactivateUser(User user);

    Page<UserResDto> findAllUsers(UserSearchReqDto userSearchReqDto, Pageable pageable);
}
