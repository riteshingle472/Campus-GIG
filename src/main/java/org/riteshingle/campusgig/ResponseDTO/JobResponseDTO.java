package org.riteshingle.campusgig.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate deadline;
    private BigDecimal budget;
    private String category;
    private String jobStatus;
    private String experience;
    private LocalDateTime publishAt;
    private String clientFirstName;
    private String clientLastName;
}
