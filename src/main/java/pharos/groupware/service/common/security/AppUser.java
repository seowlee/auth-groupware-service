package pharos.groupware.service.common.security;


import pharos.groupware.service.common.enums.UserRoleEnum;

import java.io.Serializable;
import java.util.UUID;


public record AppUser(Long id, UUID userUuid, String username, UserRoleEnum role) implements Serializable {
}
