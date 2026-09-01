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
    @Size(min = 8,message = "Password must be at least 8 digit Long")
    private String password;

    @ElementCollection(targetClass = Roles.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
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
