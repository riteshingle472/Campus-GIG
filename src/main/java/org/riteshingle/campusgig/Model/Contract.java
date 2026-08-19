package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_contract")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Agreement Amount must required ..")
    @Column(nullable = false)
    private BigDecimal agreementAmount;

    @ManyToOne
    @JoinColumn(name = "job_id",updatable = false,nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "gig_id",nullable = false,updatable = false)
    private GIG gig;

    @ManyToOne
    @JoinColumn(name = "client_id",nullable = false,updatable = false)
    private UserEntity client;

    @ManyToOne
    @JoinColumn(name = "job_application_id",nullable = false,updatable = false)
    private JobApplication jobApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus contractStatus;

    @NotNull(message = "Delivery date must required..")
    @Column(nullable = false)
    private LocalDate expectedDeliveryDate;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt;
}
