package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.ExperienceLevel;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Enum.WorkMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_job")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Job Title is required")
    @Size(min = 3,max = 200, message = "Job Title must be between 3 to 100 character")
    private String  title;

    @Column(nullable = false)
    @NotBlank(message = "Job Description is required..")
    private String  description;

    @Column(nullable = false)
    @NotNull(message = "Job Budget is required..")
    @DecimalMin(value = "0.0", message = "Job budget must more than 0 or 0")
    private BigDecimal budget;

    @Column(nullable = false)
    @NotNull(message = "Deadline is required..")
    private LocalDate deadline;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;  // OPEN , CLOSE , (DELETED -> Client)

    @ManyToOne
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private UserEntity user;

    @CreationTimestamp
    @Column(updatable = false,nullable = false)
    private LocalDateTime publishAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
