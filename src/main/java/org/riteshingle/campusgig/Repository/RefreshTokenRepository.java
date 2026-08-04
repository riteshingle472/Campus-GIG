package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.RefreshToken;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByUser(UserEntity user);
}
