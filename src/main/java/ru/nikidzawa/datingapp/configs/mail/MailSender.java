package ru.nikidzawa.datingapp.configs.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MailSender {
    @Autowired
    private JavaMailSender javaMailSender;

    @SneakyThrows
    public void sendMessage(String mail, String imagePath) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("nikidzawa@mail.ru");
        helper.setTo(mail);
        helper.setSubject("Билет на мероприятие");
        helper.setText("Привет, здесь ваш билет на мероприятие!");

        FileSystemResource file = new FileSystemResource(new File(imagePath));
        helper.addAttachment("Ticket.png", file);

        javaMailSender.send(message);
    }
}
