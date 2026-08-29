package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    @Query("""
        SELECT DISTINCT u
        FROM UserEntity u
        LEFT JOIN FETCH u.roles
        WHERE u.email = :email
        """)
    Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE :role MEMBER OF u.roles AND u.createdAt >= :fromDate AND u.createdAt < :toDate")
    Long findTotalUserByStatus(@Param("role") Roles roles, @Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);

    @Query("""
                SELECT FUNCTION('DATE', u.createdAt), COUNT(u)
                FROM UserEntity u
                WHERE :role MEMBER OF u.roles
                AND u.createdAt >= :fromDate
                AND u.createdAt < :toDate
                GROUP BY FUNCTION('DATE', u.createdAt)
                ORDER BY FUNCTION('DATE', u.createdAt)
            """)
    List<Object[]> getUserGrowth(
            @Param("role") Roles role,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
                SELECT FUNCTION('DATE', u.createdAt), COUNT(u)
                FROM UserEntity u
                WHERE :role MEMBER OF u.roles
                AND u.createdAt >= :fromDate
                AND u.createdAt < :toDate
                GROUP BY FUNCTION('DATE', u.createdAt)
                ORDER BY FUNCTION('DATE', u.createdAt)
            """)
    List<Object[]> getClientGrowth(
            @Param("role") Roles role,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
