package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Admin;
import org.riteshingle.campusgig.Model.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken ,Long>{
    Optional<AdminRefreshToken> findByAdmin(Admin admin);

}
