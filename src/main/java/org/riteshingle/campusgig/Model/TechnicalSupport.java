package org.riteshingle.campusgig.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.riteshingle.campusgig.Enum.TechnicalSupportStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tbl_technical_support")
public class TechnicalSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subject must required..")
    @Column(nullable = false,updatable = false)
    @Size(min = 10,max = 100,message = "Subject must be between 10 to 100 character")
    private String subject;

    @NotBlank(message = "Description must required..")
    @Column(nullable = false,updatable = false)
    @Size(min = 10,max = 500,message = "Description must be between 10 to 500 character")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Technical Support status is mi=ust required..")
    private TechnicalSupportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private UserEntity reporter;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.status = TechnicalSupportStatus.PENDING;
    }
}
