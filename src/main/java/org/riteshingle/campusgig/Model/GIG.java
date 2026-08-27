package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.JobCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_gig")
public class GIG {

    //    Primary unique key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    User mapping
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    //    Availability staus GIG can change it anytime when hhe don't want to work for temporary
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvailabilityStatus availabilityStatus;

    //    Job Category which types of skill GIG have
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobCategory title;

    //    GIG description
    @Column(nullable = false)
    @NotBlank(message = "Title must required..")
    private String description;

    @NotBlank(message = "College is required")
    @Size(min = 2,max = 100,message = "College name must be between 2 and 100 characters")
    private String college;

    @NotBlank(message = "Department is required")
    @Size(min = 2,max = 100,message = "Department must be between 2 and 100 characters")
    private String department;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 8, message = "Semester must not exceed 8")
    private Integer semester;

    //    GIG skills list
    @ToString.Exclude
    @OneToMany(mappedBy = "gig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSkills> userSkills = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateAt;
}
