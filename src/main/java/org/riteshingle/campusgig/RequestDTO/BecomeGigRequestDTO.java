package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.util.List;

@Data
public class BecomeGigRequestDTO {
    private String title;
    private String availabilityStatus;
    private String jobCategory;
    private String description;
    private List<Long> skillsId;
}
