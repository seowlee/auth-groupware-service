package pharos.groupware.service.auth.service;

import pharos.groupware.service.auth.dto.CreateUserReqDTO;

public interface LocalAuthService {
    String createUser(CreateUserReqDTO reqDTO);

    void deleteUser(String userId);
}
