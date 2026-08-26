package org.riteshingle.campusgig.ResponseDTO;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String message,
        boolean read,
        LocalDateTime sentAt )
{}