package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminJobApplicationResponseDTO {
    private Long id;
    private AdminJobResponseDTO jobResponseDTO;
    private AdminGigResponseDTO gigResponseDTO;
    private BigDecimal proposedAmount;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;
}