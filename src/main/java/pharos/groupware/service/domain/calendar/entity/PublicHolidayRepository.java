package pharos.groupware.service.domain.calendar.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findAllByYear(Integer year);

    @Query("select ph.holidayDate from PublicHoliday ph where ph.year = :year")
    Set<LocalDate> findAllDatesByYear(@Param("year") int year);

    Optional<PublicHoliday> findByHolidayDate(LocalDate date);

    void deleteAllByYear(Integer year);
}
