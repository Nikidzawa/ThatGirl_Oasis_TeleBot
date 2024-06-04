package ru.nikidzawa.datingapp.api.internal.controllers.mail;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.exceptions.MailException;
import ru.nikidzawa.datingapp.configs.mail.MailSender;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/mail/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailController {

    MailSender mailSender;

    @PostMapping("send/{mail}/{message}")
    public ResponseEntity<?> sendMail (@PathVariable String mail,
                                       @PathVariable String message) {
        try {
            mailSender.sendTextMessage(mail, message);
        } catch (Exception ex) {
            throw new MailException("Не удалось отправить сообщение");
        }
        return ResponseEntity.ok().build();
    }
}
