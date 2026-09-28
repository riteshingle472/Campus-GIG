package org.riteshingle.campusgig.RequestDTO;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TechnicalSupportRequestDTO {
    @NotBlank(message = "Subject must required..")
    @Size(min = 10,max = 100,message = "Subject must be between 10 to 100 character")
    private String subject;

    @NotBlank(message = "Description must required..")
    @Size(min = 10,max = 500,message = "Description must be between 10 to 500 character")
    private String description;
}
