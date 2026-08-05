package org.riteshingle.campusgig.RequestDTO;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteProfileRequestDTO {
    private String phoneNumber;
    private String college;
    private String department;
    private String profileImage;
    private String shortBio;
    private Integer semester;
    private String availableStatus;
    private LocalDate dob;
}
