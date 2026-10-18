package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private Double averageRating;
    private Long totalRating;
    private String email;
    private String profileImage;
    private LocalDate dob;
    private Boolean isVerified;
    List<ReviewResponseDTO> reviewResponseDTOS;
    private LocalDateTime createdAt;

}
