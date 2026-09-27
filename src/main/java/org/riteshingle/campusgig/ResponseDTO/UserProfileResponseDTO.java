package org.riteshingle.campusgig.ResponseDTO;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.riteshingle.campusgig.Enum.Roles;
import java.util.Set;

@Data
@Builder
public class UserProfileResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Double averageRating;
    private Long totalRatings;
    private LocalDate dob;
    private String profileImage;
    private LocalDateTime createdAt;
    private Boolean isVerified;
    private Set<Roles> roles;
}
