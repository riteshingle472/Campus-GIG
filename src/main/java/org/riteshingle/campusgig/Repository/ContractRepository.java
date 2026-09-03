package org.riteshingle.campusgig.Repository;

import org.antlr.v4.runtime.atn.SemanticContext;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByJobApplicationId(Long applicationId);

    @Query("""
            SELECT c
            FROM Contract c
            WHERE (c.client = :user OR c.gig.user = :user)
              AND (:status IS NULL OR c.contractStatus = :status)
              AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
              AND (:toDate IS NULL OR c.createdAt <= :toDate)
            """)
    List<Contract> findMyContracts(
            @Param("user") UserEntity user,
            @Param("status") ContractStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM Contract c WHERE  c.createdAt >= :fromDate AND c.createdAt <= :toDate AND (:status IS NULL OR c.contractStatus = :status)")
    Long findTotalContractByStatus(@Param("status") ContractStatus contractStatus, @Param("fromDate") LocalDateTime from, @Param("toDate") LocalDateTime to);
}
