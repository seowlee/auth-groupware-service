package pharos.groupware.service.team.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUserUuid(UUID userUUID);

    boolean existsByUserUuid(UUID userUuid);  // Repository에 선언 필요

    void deleteByUserUuid(UUID userUuid);

    @Query("""
            SELECT u FROM User u
            WHERE EXTRACT(MONTH FROM u.joinedDate) = :month
            AND EXTRACT(DAY FROM u.joinedDate) = :day
            """)
    List<User> findAllWithJoinDateMonthDay(@Param("month") int month, @Param("day") int day);

}
