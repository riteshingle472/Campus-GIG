package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.Conversation;
import org.riteshingle.campusgig.Model.Message;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.ConversationRepository;
import org.riteshingle.campusgig.Repository.MessageRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.SendMessageRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.MessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserEntityRepository userEntityRepository;


    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequestDTO sendMessageRequestDTO, Principal principal) {

        System.out.println("========== CHAT MESSAGE ==========");

        System.out.println("Conversation ID = "+conversationId);

        System.out.println("Message = "+sendMessageRequestDTO.message());

        System.out.println("Principal = " + principal);

//        Get Current Logged-in user
        if (principal == null) {
            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        String email = principal.getName();

        UserEntity currentProfile = userEntityRepository.findByEmail(email)
                .orElseThrow(() ->new RuntimeException("User not found"));

//        Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found ..."));

//        Find Contract
        Contract contract = conversation.getContract();

        if (contract == null)
            throw new RuntimeException("Conversation is not linked with contract");

//        Check contract is authorized
        if (contract.getContractStatus() != ContractStatus.PENDING) {
            throw new RuntimeException(
                    "Chat is available only for active contract"
            );
        }

        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig) throw new RuntimeException("You are not a participant of this conversation");

        Message message = Message.builder()
                .message(sendMessageRequestDTO.message())
                .sender(currentProfile)
                .conversation(conversation)
                .build();

        Message saveMessage = messageRepository.save(message);

        MessageResponse response = new MessageResponse(
                saveMessage.getId(),
                conversation.getId(),
                currentProfile.getId(),
                saveMessage.getMessage(),
                saveMessage.isRead(),
                saveMessage.getSentAt()
        );

        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, response);
        return response;
    }

    @Transactional
    public List<MessageResponse> getMessage(Long conversationId, UserEntity currentProfile) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Contract contract = conversation.getContract();

        if (contract == null)
            throw new RuntimeException("Contract not found");

        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig)
            throw new RuntimeException("You are not a participant of this conversation");

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(message -> new MessageResponse(
                                message.getId(),
                                conversationId,
                                message.getSender().getId(),
                                message.getMessage(),
                                message.isRead(),
                                message.getSentAt())
                )
                .toList();
    }
}
