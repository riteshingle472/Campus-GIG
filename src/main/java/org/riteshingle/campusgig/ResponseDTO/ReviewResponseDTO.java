package org.riteshingle.campusgig.ResponseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponseDTO {
    private String name;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
