package pharos.groupware.service.domain.calendar.service;

public interface PublicHolidaySyncService {
    void syncYear(int year);

    void syncNextYear();
}
