package pharos.groupware.service.team.service;

import pharos.groupware.service.admin.dto.CreateUserReqDto;

public interface UserService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(String userId);
}
