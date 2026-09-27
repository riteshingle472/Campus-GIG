package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Enum.NotificationType;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.InvalidStatusException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Exception.UnauthorizedException;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.Conversation;
import org.riteshingle.campusgig.Model.Message;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.ConversationRepository;
import org.riteshingle.campusgig.Repository.MessageRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.SendMessageRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.MessageResponse;
import org.riteshingle.campusgig.ResponseDTO.TypingEvent;
import org.riteshingle.campusgig.ResponseDTO.ReadEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserEntityRepository userEntityRepository;
    private final NotificationService notificationService;


    @Transactional
    public void sendMessage(Long conversationId, SendMessageRequestDTO sendMessageRequestDTO, Principal principal) {
//        Get Current Logged-in user
        if (principal == null) throw new UnauthorizedException("User is not authenticated");

//        Get email from Principal
        String email = principal.getName();

//        Get profile by email
        UserEntity currentProfile = userEntityRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));

//        Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found ..."));

//        Check contract is authorized
        if (conversation.getContract() == null)
            throw new ResourceNotFoundException("Conversation is not linked with contract");

//        Get Contract from Conversation
        Contract contract = conversation.getContract();

//        Check Contract Status
//        Chat Only Allowed for Active Contract
        if (contract.getContractStatus() == ContractStatus.WITHDRAWN || contract.getContractStatus() == ContractStatus.CANCEL)
            throw new InvalidStatusException("Chat is available only for active contract");

//        Check Client and GIG Authority
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig) throw new ForbiddenException("You are not a participant of this conversation");

//        Create message entity and save it in DB
        Message message = Message.builder()
                .message(sendMessageRequestDTO.message())
                .sender(currentProfile)
                .conversation(conversation)
                .build();

        Message saveMessage = messageRepository.save(message);

//        Convert Message in DTO
        MessageResponse response = new MessageResponse(
                saveMessage.getId(),
                conversation.getId(),
                currentProfile.getId(),
                saveMessage.getMessage(),
                saveMessage.isRead(),
                saveMessage.getSentAt()
        );

//        Get Recipient for sending notification
        UserEntity recipient = isClient ? contract.getGig().getUser() : contract.getClient();

//        Sent notification
        notificationService.notify(
                recipient,NotificationType.NEW_MESSAGE, "New Message",
                currentProfile.getFirstName() + " sent you a new message.",conversationId
        );
//        Broadcast message to conversation subscriber
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, response);
    }

//    Get Chat history
    @Transactional
    public List<MessageResponse> getMessage(Long conversationId, UserEntity currentProfile) {
//        Fetch Conversation by ID
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getContract() == null) throw new ResourceNotFoundException("Contract not found");

//        Get Contract from conversation
        Contract contract = conversation.getContract();

//        Client and GIG Authority
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You are not a participant of this conversation");

//        Return Chat history in List
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

//    Typing Indicator
    public void broadcastTyping(Long conversationId, Principal principal) {
//        Get Email form Principal
        String email = principal.getName();
//        Get User by email
        UserEntity user = userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

//        Broadcast Typing event to conversation id subscriber
        messagingTemplate.convertAndSend(
                "/topic/chat/" + conversationId + "/typing",
                new TypingEvent(user.getId(), user.getFirstName())
        );
    }

//    Mark As Read
    @Transactional
    public void markAsRead(Long conversationId, Principal principal) {
//        Get Email from Principal
        String email = principal.getName();
//        Get User by email
        UserEntity currentProfile = userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

//        mark read to all unread messages
        int updated = messageRepository.markAllAsRead(conversationId, currentProfile.getId());

//        read message (update) grater than 0
//        broadcast read event to conversation id subscriber
        if (updated > 0) {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + conversationId + "/read",
                    new ReadEvent(conversationId, currentProfile.getId())
            );
        }
    }
}
