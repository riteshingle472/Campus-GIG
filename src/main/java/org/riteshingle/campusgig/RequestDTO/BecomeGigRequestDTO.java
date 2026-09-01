package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BecomeGigRequestDTO {
    private String title;
    private String jobCategory;
    private String availabilityStatus;
    private String description;
    private String college;
    private String department;
    private Integer semester;
    private String availableStatus;
    private LocalDate dob;
    private List<Long> skillsId;
}
