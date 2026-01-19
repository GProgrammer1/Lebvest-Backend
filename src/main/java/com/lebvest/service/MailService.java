package com.lebvest.service;

import com.lebvest.model.dto.Attachment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@org.springframework.context.annotation.Profile("!dev")
@RequiredArgsConstructor
public class MailService implements IMailService {

    private final JavaMailSender mailSender;

    @Async("taskExecutor")
    public void sendSimpleMail(String to, String subject, String text) {
        var message = mailSender.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // in MailService
    @Async("taskExecutor")
    public void sendHtmlMail(
            String to,
            String subject,
            String htmlContent,
            Attachment... attachments
    ) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            for (Attachment a : attachments) {
                helper.addAttachment(
                        a.filename(),
                        new ByteArrayResource(a.data()),
                        a.contentType()
                );
            }

            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to + " with subject: " + subject);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + to + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email with attachments", e);
        } catch (Exception e) {
            System.err.println("Unexpected error sending email to: " + to + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String loadAndFormatEmailTemplate(Map<String, String> placeholders, String fileName) {
        try {
            // Load from resources/static
            var resource = new ClassPathResource("/static/" + fileName + ".html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Replace all placeholders like {{name}} with actual values
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            return template;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load CompanyRegistrationEmail.html", e);
        }
    }

}
