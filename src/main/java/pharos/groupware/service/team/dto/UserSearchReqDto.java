package pharos.groupware.service.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserSearchReqDto {
    @Schema(description = "검색 키워드 (이름, 이메일 등)", example = "member")
    private String keyword;

    @Schema(description = "팀(부서)")
    private Long teamId;
    
    @Schema(description = "역할 필터 (ex: TEAM_LEADER, TEAM_MEMBER)", example = "TEAM_MEMBER")
    private String role;

    @Schema(description = "계정 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;
}
