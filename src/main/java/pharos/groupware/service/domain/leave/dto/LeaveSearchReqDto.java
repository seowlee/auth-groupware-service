package pharos.groupware.service.domain.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "연차 목록 검색 필터")
public class LeaveSearchReqDto {

//    @Schema(description = "사용자 UUID로 특정 사용자의 연차만 필터링합니다.")
//    private String userUuid;

    @Schema(description = "사용자 이름 키워드", example = "hong")
    private String keyword;

    @Schema(description = "소속 팀 ID", example = "3")
    private Long teamId;

    @Schema(description = "연차 유형", example = "ANNUAL", allowableValues = {"ANNUAL", "BIRTHDAY", "SICK", "OTHER"})
    private String type;

    @Schema(description = "연차 상태", example = "APPROVED", allowableValues = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"})
    private String status;

    @Schema(
            description = "내 것만 보기 (기본값 true)",
            example = "true",
            defaultValue = "true",
            nullable = true
    )
    private Boolean myOnly;   // 기본 true
    @Schema(description = "내 것만 보기일 때 근속 연차 선택(n…1)", nullable = true, example = "2")
    private Integer tenureYear;

    @Schema(description = "관리자/팀장 모드에서 특정 신청자만 보기", nullable = true, example = "15")
    private Long userId;
    @Schema(
            description = "기간 시작(ISO-8601, 예: 2025-01-01T00:00:00+09:00)",
            example = "2025-01-01T00:00:00+09:00",
            nullable = true
    )
    private OffsetDateTime from;

    @Schema(
            description = "기간 끝(ISO-8601, 예: 2025-12-31T23:59:59+09:00)",
            example = "2025-12-31T23:59:59+09:00",
            nullable = true
    )
    private OffsetDateTime to;
}
