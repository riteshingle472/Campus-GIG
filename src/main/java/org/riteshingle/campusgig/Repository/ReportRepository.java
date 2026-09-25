package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.ActionInitiatedBy;
import org.riteshingle.campusgig.Enum.ReportStatus;
import org.riteshingle.campusgig.Model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
//    boolean existsByContractIdActionInitiatedBy(Long contractId, ActionInitiatedBy actionInitiatedBy);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdAt >= :fromDate AND r.createdAt <= :endDate AND (:status IS NULL OR r.reportStatus = :status)")
    Long findTotalReportByStatus(@Param("status") ReportStatus reportStatus,@Param("fromDate") LocalDateTime from,@Param("endDate") LocalDateTime endTo);

    @Query("select r from Report r where r.actionInitiatedBy = :status")
    List<Report> findReports(@Param("status") ActionInitiatedBy actionInitiatedBy, Pageable pageable);

}
