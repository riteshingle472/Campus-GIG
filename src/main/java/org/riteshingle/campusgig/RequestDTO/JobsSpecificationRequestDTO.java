package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class JobsSpecificationRequestDTO {
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private LocalDate minDeadline;
    private LocalDate maxDeadline;
    private String experienceLevel;
    private String jobCategory;
}
