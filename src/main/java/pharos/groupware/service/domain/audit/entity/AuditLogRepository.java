package pharos.groupware.service.domain.audit.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
              select a from AuditLog a
              where (:action is null or a.action = :action)
                and (:status is null or a.status = :status)
                and (
                      lower(a.createdBy) like lower(concat('%', :kw, '%'))
                     or lower(a.ipAddress) like lower(concat('%', :kw, '%'))
                     or lower(a.action)    like lower(concat('%', :kw, '%'))
                     or :kw is null
                )
            """)
    Page<AuditLog> search(String kw, String action, String status, Pageable pageable);
}
