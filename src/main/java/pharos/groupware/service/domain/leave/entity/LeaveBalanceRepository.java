package pharos.groupware.service.domain.leave.entity;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
