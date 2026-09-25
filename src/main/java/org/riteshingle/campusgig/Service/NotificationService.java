package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.NotificationType;
import org.riteshingle.campusgig.Model.Notification;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.NotificationRepository;
import org.riteshingle.campusgig.ResponseDTO.NotificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthService authService;


    public void sendMail(String to , String subject ,String body){
        SimpleMailMessage javaMail =  new SimpleMailMessage();

        try {
            javaMail.setTo(to);
            javaMail.setSubject(subject);
            javaMail.setText(body);

            javaMailSender.send(javaMail);
        }catch (Exception e){
            e.printStackTrace(); // ya logger.error("Mail error: ", e);
            throw new RuntimeException("Mail sending failed: " + e.getMessage(), e);

        }
    }


    /**
     * Core entry point — every other service calls this to notify a user.
     * Saves to DB first (so it's durable even if the recipient is offline),
     * then pushes it live over STOMP if they happen to be connected.
     */
    public void notify(UserEntity recipient, NotificationType type, String title, String message, Long referenceId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationResponseDTO dto = toDto(saved);

        // Per-user private queue — see WebSocketConfiguration note below for
        // why this needs enableSimpleBroker("/topic", "/user") and a
        // Principal-based destination.
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                dto
        );
    }

    public Page<NotificationResponseDTO> getMyNotifications(Pageable pageable) {
        UserEntity currentProfile = authService.getCurrentProfile();
        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(currentProfile, pageable)
                .map(this::toDto);
    }

    public long getUnreadCount() {
        UserEntity currentProfile = authService.getCurrentProfile();
        return notificationRepository.countByRecipientAndIsReadFalse(currentProfile);
    }

    public void markAllAsRead() {
        UserEntity currentProfile = authService.getCurrentProfile();
        notificationRepository.markAllAsRead(currentProfile);
    }

    private NotificationResponseDTO toDto(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
