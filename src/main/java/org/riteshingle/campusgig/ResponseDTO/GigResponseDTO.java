package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GigResponseDTO {
    private Long id;
    private String gigFirstName;
    private String gigLastName;
    private String gigEmail;
    private String gigPhoneNumber;
    private String jobCategory;
    private String title;
    private String college;
    private String department;
    private Integer semester;
    private String description;
    private AvailabilityStatus availabilityStatus;
    private List<SkillResponseDTO> gigSkills;
}