package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final JavaMailSender javaMailSender;

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
}
