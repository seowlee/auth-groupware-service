package pharos.groupware.service.domain.calendar.service;

import pharos.groupware.service.domain.calendar.dto.SyncReportResDto;

public interface PublicHolidaySyncService {
    void syncYear(int year);

    void syncNextYear();

    SyncReportResDto addMissingOnly(int year);
}
