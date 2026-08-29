package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.Roles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First Name is required..")
    @Size(min=2,max = 50,message = "First Name must be between 2 to 50 characters..")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "Last Name is required..")
    @Size(min=2,max = 50,message = "Last Name must be between 2 to 50 characters..")
    @Column(nullable = false)
    private String lastName;

    @Email(message = "Enter a valid email..")
    @NotBlank(message = "Email is required..")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "Password is  required ...")
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @NotNull(message = "Is verified can not be null")
    private Boolean isVerified;

    @ElementCollection(targetClass = Roles.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    private Set<Roles> roles;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private GIG gig;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false)
    @Pattern(regexp = "^[6-9]\\d{9}$",message = "Enter a valid 10-digit Indian phone number")
    private String phoneNumber;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImage;

    @NotNull(message = "Date of birth is required")
    @Column(nullable = false)
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Builder.Default
    @Column(nullable = false)
    @NotNull(message = "Average Rating must required..")
    private Long totalRatingSum = 0L;

    @Builder.Default
    @Column(nullable = false)
    @NotNull(message = "Total Rating must required..")
    private Long totalRatings = 0L;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.isVerified = false;
    }

    @Transient
    public Double getAverageRating() {
        if (totalRatings == null || totalRatings == 0) return 0.0;
        return Math.round(((double) totalRatingSum / totalRatings) * 10.0) / 10.0;
    }
}
