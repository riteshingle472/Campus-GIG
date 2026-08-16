package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobRequestDTO {
    private String draftId;
    private String title;
    private String description;
    private String experienceLevel;
    private String workMode;
    private String jobCategory;
    private LocalDate deadline;
    private BigDecimal budget;
    private String jobStatus;
}
