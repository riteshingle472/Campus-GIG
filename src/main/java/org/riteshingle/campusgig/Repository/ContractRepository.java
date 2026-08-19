package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract , Long> {
    Optional<Contract> findByApplicationId(Long applicationId);

}
