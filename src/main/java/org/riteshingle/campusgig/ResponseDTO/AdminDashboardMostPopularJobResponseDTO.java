package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.riteshingle.campusgig.Enum.JobCategory;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardMostPopularJobResponseDTO {
    private Long count;
    private JobCategory jobCategory;
}
