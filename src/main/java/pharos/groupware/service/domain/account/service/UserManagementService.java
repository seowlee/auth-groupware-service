package pharos.groupware.service.domain.account.service;


import pharos.groupware.service.domain.account.dto.CreateUserReqDto;
import pharos.groupware.service.domain.account.dto.PendingUserReqDto;
import pharos.groupware.service.domain.account.dto.UpdateUserByAdminReqDto;
import pharos.groupware.service.domain.account.dto.UserApplicantResDto;

import java.util.List;
import java.util.UUID;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(String userId);

//    String deactivateUser(String userId);

//    void reactivateUser(User user);

    String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto);

//    void approvePendingUser(java.util.UUID userUuid);

    void registerOrLinkSocialUser(PendingUserReqDto dto);

    void deleteUsersOlderThanDays(int i);

    List<UserApplicantResDto> findAllApplicants(String q);
}
