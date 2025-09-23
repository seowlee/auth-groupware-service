package pharos.groupware.service.domain.holiday.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pharos.groupware.service.domain.holiday.dto.HolidayKey;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findAllByYear(Integer year);

    @Query("select ph.holidayDate from PublicHoliday ph where ph.year = :year")
    Set<LocalDate> findAllDatesByYear(@Param("year") int year);

    @Query("select new pharos.groupware.service.domain.holiday.dto.HolidayKey(h.holidayDate, h.seq) " +
            "from PublicHoliday h where h.year = :year")
    Set<HolidayKey> findAllKeysByYear(int year);

    Optional<PublicHoliday> findByHolidayDate(LocalDate date);

    void deleteAllByYear(Integer year);

    // 달력 범위로 조회 (FullCalendar가 start/end 전달)
    List<PublicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);

    // 특정 연도 전체(연 단위 캐시용)
    List<PublicHoliday> findByYearOrderByHolidayDateAsc(Integer year);
}
