package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.ExperienceLevel;
import org.riteshingle.campusgig.Enum.JobCategory;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Enum.WorkMode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponseDTO {
    private String jobTitle;
    private LocalDate deadline;
    private BigDecimal budget;
    private JobCategory category;
    private WorkMode workMode;
    private JobStatus jobStatus;
    private ExperienceLevel experienceLevel;
}
