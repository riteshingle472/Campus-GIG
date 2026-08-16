package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateJobApplicationRequestDTO {
    private String coverLetter;
    private BigDecimal bidAmount;
    private LocalDate deliveryDate;
}
