package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.riteshingle.campusgig.Enum.ContractStatus;
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


    @Transactional
    public void sendMessage(Long conversationId, SendMessageRequestDTO sendMessageRequestDTO, Principal principal) {

        log.info("========== CHAT MESSAGE ==========");
        log.info("Conversation ID = {}", conversationId);
        log.info("Message = {}", sendMessageRequestDTO.message());
        log.info("Principal = {}", principal);

//        Get Current Logged-in user
        if(principal == null) throw new UnauthorizedException("User is not authenticated");
//        Get Email From Principal
        String email = principal.getName();

//        Get User by Email
        UserEntity currentProfile = userEntityRepository.findByEmail(email).orElseThrow(() ->new ResourceNotFoundException("User not found"));

//        Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found ..."));

//        Find Contract
        Contract contract = conversation.getContract();

        if (contract == null)
            throw new ResourceNotFoundException("Conversation is not linked with contract");

//        Check contract is authorized
        if (contract.getContractStatus() == ContractStatus.WITHDRAWN || contract.getContractStatus() == ContractStatus.CANCEL)
            throw new InvalidStatusException("Chat is available only for active contract");

//        Check Client and GIG authorization From Contract
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig) throw new ForbiddenException("You are not a participant of this conversation");

//        Save Message in DB
        Message saveMessage = Message.builder()
                .message(sendMessageRequestDTO.message())
                .sender(currentProfile)
                .conversation(conversation)
                .build();

        saveMessage = messageRepository.save(saveMessage);

        MessageResponse response = new MessageResponse(
                saveMessage.getId(),
                conversation.getId(),
                currentProfile.getId(),
                saveMessage.getMessage(),
                saveMessage.isRead(),
                saveMessage.getSentAt()
        );

//        Delivered message to the Subscriber
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, response);
    }

//    Chat History
    @Transactional
    public List<MessageResponse> getMessage(Long conversationId, UserEntity currentProfile) {
//        Get Conversation by Conversation ID
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

//        Get contract from conversation
        Contract contract = conversation.getContract();
//        Check Contract is not null
        if (contract == null) throw new ResourceNotFoundException("Contract not found");

//        Check Client and GIG authorization from contract
        boolean isClient = contract.getClient().getId().equals(currentProfile.getId());
        boolean isGig = currentProfile.getGig() != null && contract.getGig().getId().equals(currentProfile.getGig().getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You are not a participant of this conversation");

//        Fetch All Chat Messages
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
