package pharos.groupware.service.admin.service;


import pharos.groupware.service.admin.dto.CreateUserReqDto;
import pharos.groupware.service.admin.dto.UpdateUserByAdminReqDto;

import java.util.UUID;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(String userId);

    String deactivateUser(String userId);

    String updateUser(UUID uuid, UpdateUserByAdminReqDto reqDto);

    void approvePendingUser(java.util.UUID userUuid);
}
