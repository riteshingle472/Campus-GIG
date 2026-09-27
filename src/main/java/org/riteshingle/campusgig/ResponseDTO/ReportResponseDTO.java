package org.riteshingle.campusgig.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponseDTO {
    private Long id;
    private String jobTitle;
    private String otherPartyName;
    private String reportReason;
    private String reportStatus;
    private String description;
    private String adminRemark;
    private boolean filedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime resolveAt;
}