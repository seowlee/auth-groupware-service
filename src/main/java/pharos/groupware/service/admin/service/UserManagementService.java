package pharos.groupware.service.admin.service;


import pharos.groupware.service.admin.dto.CreateUserReqDto;

public interface UserManagementService {
    String createUser(CreateUserReqDto reqDTO);

    String deleteUser(String userId);
}
