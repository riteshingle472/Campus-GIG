package org.riteshingle.campusgig.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardCardStatsResponseDTO {
    private Long totalUser;
    private Long totalGIG;
    private Long totalClient;
    private Long totalJob;
    private Long totalJobApplication;
    private Long totalContract;
    private Long totalReport;
}
