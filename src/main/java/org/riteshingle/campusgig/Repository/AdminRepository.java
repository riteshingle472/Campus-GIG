package org.riteshingle.campusgig.Repository;

import lombok.Data;
import org.riteshingle.campusgig.Model.Admin;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

}
