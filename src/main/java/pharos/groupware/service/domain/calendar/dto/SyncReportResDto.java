package pharos.groupware.service.domain.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SyncReportResDto {
    private int year;
    private String status; // OK/FAIL
    private String message;
    private int inserted;
    private int updated;
    private int apiCount;

    public static SyncReportResDto ok(int year, int inserted, int updated, int apiCount) {
        return new SyncReportResDto(year, "OK", null, inserted, updated, apiCount);
    }

    public static SyncReportResDto fail(int year, String msg) {
        return new SyncReportResDto(year, "FAIL", msg, 0, 0, 0);
    }
}
