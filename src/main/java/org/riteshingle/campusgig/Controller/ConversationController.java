package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.RequestDTO.SendMessageRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.MessageResponse;
import org.riteshingle.campusgig.Service.AuthService;
import org.riteshingle.campusgig.Service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatService chatService;
    private final AuthService authService;

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long conversationId){
        UserEntity currentUser = authService.getCurrentProfile();
        return ResponseEntity.ok(chatService.getMessage(conversationId, currentUser));
    }
}
