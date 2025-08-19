package pharos.groupware.service.domain.admin;


import pharos.groupware.service.domain.admin.dto.CreateUserReqDto;
import pharos.groupware.service.domain.admin.dto.PendingUserDto;
import pharos.groupware.service.domain.admin.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.team.entity.User;

import java.util.UUID;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(String userId);

    String deactivateUser(String userId);

    void reactivateUser(User user);

    String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto);

    void approvePendingUser(java.util.UUID userUuid);

    void registerOrLinkSocialUser(PendingUserDto dto);

    void deleteUsersOlderThanDays(int i);
}
