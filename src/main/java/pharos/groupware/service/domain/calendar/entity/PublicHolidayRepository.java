package pharos.groupware.service.domain.calendar.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {
    List<PublicHoliday> findAllByYear(Integer year);

    Optional<PublicHoliday> findByHolidayDate(LocalDate date);

    void deleteAllByYear(Integer year);
}
