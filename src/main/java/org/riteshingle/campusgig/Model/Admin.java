package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.riteshingle.campusgig.Enum.AdminAccessStatus;
import org.riteshingle.campusgig.Enum.AdminStatus;
import org.riteshingle.campusgig.Enum.Roles;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,updatable = false)
    @NotBlank(message = "Email is required..")
    private String email;

    @Column(nullable = false,updatable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8,message = "Password must be at least 8 digit")
    private String password;

    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Column(nullable = false)
    @NotBlank(message = "Contact number is required")
    private String contactNo;

    @ElementCollection(targetClass = Roles.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "admin_roles",
            joinColumns = @JoinColumn(name = "admin_id")
    )
    @Column(name = "role")
    private Set<Roles> roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminAccessStatus adminAccessStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminStatus adminStatus;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;
}
