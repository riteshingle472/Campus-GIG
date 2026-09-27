package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.SendMessageRequestDTO;
import org.riteshingle.campusgig.Service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

//    Send Message
    @MessageMapping("/chat/{conversationId}")
    public void sendMessage(@DestinationVariable Long conversationId, @Payload SendMessageRequestDTO request ,  Principal principal) {
        chatService.sendMessage(conversationId, request, principal);
    }

//    Typing Indicator
    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(@DestinationVariable Long conversationId, Principal principal) {
        chatService.broadcastTyping(conversationId, principal);
    }

//    Mark Read Messages
    @MessageMapping("/chat/{conversationId}/read")
    public void markRead(@DestinationVariable Long conversationId, Principal principal) {
        chatService.markAsRead(conversationId, principal);
    }

}
