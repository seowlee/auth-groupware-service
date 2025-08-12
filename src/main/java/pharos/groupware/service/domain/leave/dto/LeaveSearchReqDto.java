package pharos.groupware.service.domain.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "연차 목록 검색 필터")
public class LeaveSearchReqDto {

    @Schema(description = "사용자 UUID로 특정 사용자의 연차만 필터링합니다.")
    private String userUuid;

    @Schema(description = "사용자 이름 키워드", example = "hong")
    private String keyword;

    @Schema(description = "소속 팀 ID", example = "3")
    private Long teamId;

    @Schema(description = "연차 유형", example = "ANNUAL", allowableValues = {"ANNUAL", "BIRTHDAY", "SICK", "OTHER"})
    private String type;

    @Schema(description = "연차 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"})
    private String status;


}
