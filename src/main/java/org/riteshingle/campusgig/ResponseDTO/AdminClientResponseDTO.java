package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClientResponseDTO{
       private Long id;
       private String firstName;
       private String lastName;
       private String email;
       private String phoneNumber;
       private String role;
       private String profileImage;
       private AvailabilityStatus availabilityStatus;
       private LocalDateTime createdAt;
}
