package org.riteshingle.campusgig.Repository;

import org.antlr.v4.runtime.atn.SemanticContext;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract , Long> {
    Optional<Contract> findByJobApplicationId(Long applicationId);

    @Query("SELECT c FROM Contract c WHERE c.client = :user OR c.gig.user = :user")
    List<Contract> findMyContracts(@Param("user") UserEntity user);
}
