package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

@Data
public class ReportRequestDTO {
    private Long contractId;
    private String description;
    private String reportReasonStatus;
}
