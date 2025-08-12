package pharos.groupware.service.domain.leave.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pharos.groupware.service.common.enums.LeaveStatusEnum;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    @Query("""
            SELECT l FROM Leave l
            LEFT JOIN FETCH l.user u
            WHERE (:uuid IS NULL OR u.userUuid = :uuid)
              AND (:teamId IS NULL OR u.team.id = :teamId)
              AND ( LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR :keyword IS NULL )
              AND (:type IS NULL OR l.leaveType = :type)
              AND (:status IS NULL OR l.status = :status)
            """)
    Page<Leave> findAllBySearchFilter(
            @Param("uuid") String uuid,
            @Param("keyword") String keyword,
            @Param("teamId") Long teamId,
            @Param("type") LeaveTypeEnum type,
            @Param("status") LeaveStatusEnum status,
            Pageable pageable
    );
}
