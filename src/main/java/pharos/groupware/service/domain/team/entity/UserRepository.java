package pharos.groupware.service.domain.team.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pharos.groupware.service.common.enums.UserRoleEnum;
import pharos.groupware.service.common.enums.UserStatusEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUserUuid(UUID userUUID);

    boolean existsByUserUuid(UUID userUuid);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'ACTIVE'
            AND EXTRACT(MONTH FROM u.joinedDate) = :month
            """)
    List<User> findAllActiveByHiredMonth(@Param("month") int month);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'ACTIVE'
              AND EXTRACT(MONTH FROM u.joinedDate) = :month
              AND EXTRACT(DAY FROM u.joinedDate) = :day
            """)
    List<User> findAllActiveByHiredDate(@Param("month") int month, @Param("day") int day);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'ACTIVE'
            AND u.yearNumber = 1
            """)
    List<User> findAllActiveOfFirstYearNumber();

    void deleteByUserUuid(UUID userUuid);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Query(
            value = """
                    SELECT *
                    FROM groupware.users
                    WHERE
                        (:keyword IS NULL OR LOWER(username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                         OR LOWER(email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        AND (:role IS NULL OR role = :role)
                        AND (:status IS NULL OR status = :status)
                    ORDER BY id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM groupware.users
                    WHERE
                        (:keyword IS NULL OR LOWER(username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                         OR LOWER(email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        AND (:role IS NULL OR role = :role)
                        AND (:status IS NULL OR status = :status)
                    """,
            nativeQuery = true
    )
    Page<User> searchUsersNative(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
                SELECT u FROM User u
                LEFT JOIN FETCH u.team t
                WHERE
                    ( LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR :keyword IS NULL )
                    AND (:teamId IS NULL OR t.id = :teamId)
                    AND (:role IS NULL OR u.role = :role)
                    AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> findAllBySearchFilter(
            @Param("keyword") String keyword,
            @Param("teamId") Long teamId,
            @Param("role") UserRoleEnum role,
            @Param("status") UserStatusEnum status,
            Pageable pageable
    );

    Optional<User> findByEmail(String email);

    @Query("""
                SELECT u
                  FROM User u
                 WHERE u.status = 'ACTIVE'
                   AND ( LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
                        OR :q IS NULL)
                 ORDER BY u.username ASC
            """)
    List<User> findActiveUsersForSelect(@Param("q") String q);

    @Query("""
              SELECT u FROM User u
              WHERE u.status='ACTIVE' AND u.team.id=:teamId
                AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR :q IS NULL)
              ORDER BY u.username ASC
            """)
    List<User> findActiveUsersForSelectByTeam(@Param("teamId") Long teamId, @Param("q") String q);

    Optional<User> findByKakaoSub(String kakaoSub);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmailIgnoreCase(String email);

    // kakao_sub + ACTIVE
    Optional<User> findByKakaoSubAndStatus(String kakaoSub, UserStatusEnum status);

    // phone_number + ACTIVE
    Optional<User> findByPhoneNumberAndStatus(String phoneNumber, UserStatusEnum status);

    // email(무시 대소문자) + ACTIVE
    @Query("select u from User u where lower(u.email) = lower(:email) and u.status = :status")
    Optional<User> findByEmailIgnoreCaseAndStatus(@Param("email") String email,
                                                  @Param("status") UserStatusEnum status);

    @Query("select u.userUuid from User u " +
            "where u.status = 'INACTIVE' " +
            "and u.updatedAt < :cutoff")
    List<UUID> findInactiveUserIdsOlderThan(OffsetDateTime cutoff);

    long deleteByUserUuidIn(List<UUID> chunk);

    boolean existsByUserUuidAndKakaoSubIsNotNull(java.util.UUID uuid);

}
