package pharos.groupware.service.domain.account;


import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.admin.dto.PendingUserDto;
import pharos.groupware.service.domain.admin.dto.UpdateUserByAdminReqDto;

import java.util.UUID;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(String userId);

//    String deactivateUser(String userId);

//    void reactivateUser(User user);

    String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto);

//    void approvePendingUser(java.util.UUID userUuid);

    void registerOrLinkSocialUser(PendingUserDto dto);

    void deleteUsersOlderThanDays(int i);
}
