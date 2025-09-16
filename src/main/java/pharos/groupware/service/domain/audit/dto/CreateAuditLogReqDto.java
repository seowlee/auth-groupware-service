package pharos.groupware.service.domain.audit.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateAuditLogReqDto {
    private Long userId;          // 행위자(또는 대상) 식별자
    private String ipAddress;
    private String actor;         // createdBy/updatedBy에 반영
    private String action;        // enum.name()
    private String status;        // enum.name()
    private String detailJson;    // 원시 페이로드 JSON
}
