package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.ActionInitiatedBy;
import org.riteshingle.campusgig.Enum.ReportReason;
import org.riteshingle.campusgig.Enum.ReportStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_report")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 10,max = 500,message = "Reason must be between 10 to 500 characters...")
    @Column(nullable = false,updatable = false)
    @NotBlank(message = "Reason must be required...")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,updatable = false,name = "gig_id")
    private GIG gig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,updatable = false,name = "client_id")
    private UserEntity client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,updatable = false)
    private ReportReason reportReason;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionInitiatedBy actionInitiatedBy;

    @Size(min = 10,max = 1000,message = "Admin must be between 10 to 1000 characters..")
    private String adminRemark;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime resolveAt;

}
