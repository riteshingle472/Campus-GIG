package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.JobCategory;

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
    private JobCategory jobCategory;

    //    Job Title
    @Column(nullable = false)
    @NotBlank(message = "Title must required..")
    private String title;

    //    GIG description
    @Column(nullable = false)
    @NotBlank(message = "Title must required..")
    private String description;

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
