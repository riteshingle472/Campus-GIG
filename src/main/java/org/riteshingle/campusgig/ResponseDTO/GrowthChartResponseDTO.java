package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrowthChartResponseDTO {
    private Long users;
    private Long gigs;
    private Long clients;
    private Long jobs;
    private Long applications;
    private LocalDate date;
}
