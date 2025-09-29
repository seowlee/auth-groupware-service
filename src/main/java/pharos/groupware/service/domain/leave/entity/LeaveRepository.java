package pharos.groupware.service.domain.leave.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

import java.util.Collection;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    @Query("""
            SELECT l FROM Leave l
            LEFT JOIN FETCH l.user u
            WHERE (:teamId IS NULL OR u.team.id = :teamId)
              AND ( LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR :keyword IS NULL )
              AND (:type IS NULL OR l.leaveType = :type)
              AND (:status IS NULL OR l.status = :status)
            """)
    Page<Leave> findAllBySearchFilter(
            @Param("keyword") String keyword,
            @Param("teamId") Long teamId,
            @Param("type") LeaveTypeEnum type,
            @Param("status") LeaveStatusEnum status,
            Pageable pageable
    );

    @Query("""
            select l
              from Leave l
              join fetch l.user u
             where (:teamId is null or u.team.id = :teamId)
               and (:type is null or l.leaveType = :type)
               and (coalesce(:statuses, null) is null or l.status in :statuses)
               and (l.endDt >= :start and l.startDt <= :end)
            """)
    List<Leave> findAllForCalendar(
            @Param("teamId") Long teamId,
            @Param("type") pharos.groupware.service.common.enums.LeaveTypeEnum type,
            @Param("statuses") Collection<LeaveStatusEnum> statuses,
            @Param("start") java.time.OffsetDateTime start,
            @Param("end") java.time.OffsetDateTime end
    );
}
