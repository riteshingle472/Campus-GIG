package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateJobRequestDTO {
    private String draftId;
    private String title;
    private String description;
    private String experienceLevel;
    private String workMode;
    private String category;
    private LocalDate deadline;
    private BigDecimal budget;
    private String jobStatus;
}
