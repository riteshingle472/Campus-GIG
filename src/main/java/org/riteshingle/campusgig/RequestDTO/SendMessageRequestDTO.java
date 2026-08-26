package org.riteshingle.campusgig.RequestDTO;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequestDTO(
        @NotBlank(message = "Message cannot be empty")
        String message
) {
}
