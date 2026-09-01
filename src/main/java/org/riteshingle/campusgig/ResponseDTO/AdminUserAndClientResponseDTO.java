package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserAndClientResponseDTO {
   private Long id;
   private String firstName;
   private String lastName;
   private String email;
   private String phoneNumber;
   private String profileImage;
   private Double averageRating;
   private LocalDate dob;
   private Boolean isVerified;
   private LocalDateTime createdAt;
}
