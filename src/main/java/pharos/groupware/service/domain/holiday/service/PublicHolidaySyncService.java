package pharos.groupware.service.domain.holiday.service;

import pharos.groupware.service.domain.holiday.dto.SyncReportResDto;

public interface PublicHolidaySyncService {
    void syncYear(int year);

    void syncNextYear();

    SyncReportResDto addMissingOnly(int year);
}
