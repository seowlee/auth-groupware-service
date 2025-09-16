package pharos.groupware.service.common.enums;

import lombok.Getter;

@Getter
public enum AuditActionEnum {
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,
    USER_PENDING,
    USER_DELETE_KEYCLOAK,
    LEAVE_APPLY_GRAPH_CREATE,
    LEAVE_UPDATE_GRAPH_UPDATE,
    USER_SOCIAL_LINK,
    LEAVE_CANCEL_GRAPH_DELETE
}


