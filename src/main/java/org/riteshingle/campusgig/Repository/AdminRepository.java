package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.AdminAccessStatus;
import org.riteshingle.campusgig.Model.Admin;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin,Long> {
    Optional<Admin> findByEmail(String email);

    @Query("""
        SELECT DISTINCT a
        FROM Admin a
        LEFT JOIN FETCH a.roles
        WHERE a.email = :email
        """)
    Optional<Admin> findByEmailWithRoles(@Param("email") String email);

    @Query("""
    SELECT a FROM Admin a
        WHERE
            (
                :keyword IS NULL
                OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.adminAccessStatus) LIKE UPPER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.adminStatus) LIKE UPPER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.contactNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
        AND (:status IS NULL OR a.adminAccessStatus = :status)
    """)
    List<Admin> findAdminByKeywords(@Param("status") AdminAccessStatus adminAccessStatus,@Param("keyword") String keyword, Pageable pageable);
}
