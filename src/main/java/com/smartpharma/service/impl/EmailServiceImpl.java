package com.smartpharma.service.impl;

import com.smartpharma.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from-address:}")
    private String fromAddress;

    @Override
    public void sendPlainTextEmail(String to, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) {
            // Fails closed with a clear message instead of letting JavaMailSender throw
            // an opaque authentication error when spring.mail.username was never set.
            throw new RuntimeException("Email sending is not configured for this pharmacy - contact your administrator");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email sent to {} - subject: {}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body,
                                         byte[] attachment, String attachmentFilename, String attachmentContentType) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new RuntimeException("Email sending is not configured for this pharmacy - contact your administrator");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart, required for addAttachment to work
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachment), attachmentContentType);

            mailSender.send(message);
            log.info("Email with attachment sent to {} - subject: {}", to, subject);
        } catch (jakarta.mail.MessagingException | MailException e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
