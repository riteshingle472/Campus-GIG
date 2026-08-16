package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.JobApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationSortingAndFilteringResponseDTO {
    private BigDecimal budget;
    private String coverLetter;
    private JobApplicationStatus jobApplicationStatus;
    private LocalDate deliveryDate;
    private LocalDate applyAt;
}
