package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.JobApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_job_application")
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "gig_id")
    private GIG gig;

    @DecimalMin("0")
    @Column(nullable = false)
    @NotNull(message = "Bid amount is required ..")
    private BigDecimal bidAmount;

    @NotBlank(message = "Cover letter is required..")
    @Size(min = 0,max = 5000,message = "Cover letter must be between 0 to 5000 character")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobApplicationStatus jobApplicationStatus;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDate createAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDate updateAt;
}
