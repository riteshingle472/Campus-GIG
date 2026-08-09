package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private String phoneNumber;
    private String college;
    private String department;
    private String profileImage;
    private String shortBio;
    private Integer semester;

    @Column(nullable = false)
    @NotNull(message = "Is verified can not be null")
    private Boolean isVerified;

    @Enumerated(EnumType.STRING)
    private Roles roles;

    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus;

    private LocalDate dob;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSkills> userSkills = new ArrayList<>();

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
}
