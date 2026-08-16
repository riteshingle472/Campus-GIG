package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobApplicantResponseDTO {
    private String coverLetter;
    private String jobApplicationStatus;
    private BigDecimal bidAmount;
    private LocalDate deliveryDate;
    private LocalDate applyAt;
    private GigResponseDTO gigResponseDTO;
}
