package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminReportListResponseDTO {
    private Long id;
    private String reason;
    private String description;
    private String reportStatus;
    private String reportedBy;
    private LocalDateTime createdAt;
}
