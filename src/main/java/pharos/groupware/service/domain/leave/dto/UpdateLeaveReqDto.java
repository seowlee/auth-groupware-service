package pharos.groupware.service.domain.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateLeaveReqDto {

    @Schema(description = "시작 시각 (ISO 8601 / 로컬 시각)", example = "2025-07-21T09:00:00")
    @NotNull
    private LocalDateTime startDt;

    @Schema(description = "종료 시각 (ISO 8601 / 로컬 시각)", example = "2025-07-21T18:00:00")
    @NotNull
    private LocalDateTime endDt;

    @Schema(hidden = true, description = "연차 신청 사용 일수", example = "2.500")
    private BigDecimal usedDays;

    @Schema(description = "연차 유형", example = "ANNUAL", allowableValues = {"ANNUAL", "BIRTHDAY", "SICK", "CUSTOM"})
    @NotNull
    private String leaveType;

    @Schema(description = "연차 사유", example = "휴가")
    private String reason;


}
