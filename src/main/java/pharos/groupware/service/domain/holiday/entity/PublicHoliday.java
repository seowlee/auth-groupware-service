package pharos.groupware.service.domain.holiday.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "public_holidays", schema = "groupware")
public class PublicHoliday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Size(max = 100)
    @NotNull
    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(name = "seq", nullable = false)
    private Short seq;

    @NotNull
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    public static PublicHoliday of(LocalDate date, String name, short seq, int year) {
        PublicHoliday ph = new PublicHoliday();
        ph.holidayDate = date;
        ph.holidayName = name;
        ph.seq = seq;
        ph.year = year;
        ph.createdAt = OffsetDateTime.now();
        ph.createdBy = "system";
        ph.updatedAt = ph.createdAt;
        ph.updatedBy = "system";
        return ph;
    }

}