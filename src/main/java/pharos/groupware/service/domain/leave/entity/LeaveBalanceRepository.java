package pharos.groupware.service.domain.leave.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pharos.groupware.service.common.enums.LeaveTypeEnum;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long id);

    List<LeaveBalance> findByUserIdAndYearNumber(Long id, Integer yearNumber);


    Optional<LeaveBalance> findByUserIdAndLeaveTypeAndYearNumber(
            Long userId, LeaveTypeEnum leaveType, Integer yearNumber);

    @Query("""
            select lb
            from LeaveBalance lb
            where lb.yearNumber = (
                select max(lb2.yearNumber)
                from LeaveBalance lb2
                where lb2.userId = lb.userId
            )
            and (:leaveType is null or lb.leaveType = :leaveType)
            """)
    List<LeaveBalance> findLatestYearBalances(@Param("leaveType") LeaveTypeEnum leaveType);
}
