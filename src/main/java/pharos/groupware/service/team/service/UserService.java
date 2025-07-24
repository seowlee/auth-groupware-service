package pharos.groupware.service.team.service;

import pharos.groupware.service.admin.dto.CreateUserReqDto;

import java.util.UUID;

public interface UserService {
    String createUser(CreateUserReqDto reqDTO);

    void deleteUser(UUID userUuid);

    void deactivateUser(UUID userUuid);
}
