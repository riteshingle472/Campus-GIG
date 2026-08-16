package org.riteshingle.campusgig.RequestDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobApplicationFilterAndSortingRequestDTO {
    private String applicationStatus;
    private BigDecimal minBidAmount;
    private BigDecimal maxBidAmount;
    private String sortDirection;
    private String sortByField;
}
