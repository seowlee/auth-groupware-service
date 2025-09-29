package pharos.groupware.service.domain.account.service;


import pharos.groupware.service.common.security.AppUser;
import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.PendingUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.account.dto.UserApplicantResDto;

import java.util.List;
import java.util.UUID;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO, AppUser actor);

    void deleteUser(String userId, AppUser actor);

//    String deactivateUser(String userId);

//    void reactivateUser(User user);

    String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto, AppUser actor);

//    void approvePendingUser(java.util.UUID userUuid);

    void registerOrLinkSocialUser(PendingUserReqDto dto);

    void deleteUsersOlderThanDays(int i);

    List<UserApplicantResDto> findAllApplicants(String q);
}
