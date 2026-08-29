package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Model.GIG;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GigRepository extends JpaRepository<GIG, Long> {

    @Query("""
                SELECT COUNT(g)
                FROM GIG g
                WHERE g.createdAt >= :fromDate
                AND g.createdAt < :toDate
            """)
    Long findTotalGIGByStatus(
            @Param("fromDate") LocalDateTime from,
            @Param("toDate") LocalDateTime to
    );

    @Query("""
                SELECT FUNCTION('DATE', g.createdAt), COUNT(g)
                FROM GIG g
                WHERE g.createdAt >= :fromDate
                AND g.createdAt < :toDate
                GROUP BY FUNCTION('DATE', g.createdAt)
                ORDER BY FUNCTION('DATE', g.createdAt)
            """)
    List<Object[]> getGigGrowth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
