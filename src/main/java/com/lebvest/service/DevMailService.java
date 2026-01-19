package com.lebvest.service;

import com.lebvest.model.dto.Attachment;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@Slf4j
@Service
@Profile("dev")
public class DevMailService implements IMailService {

    private GreenMail greenMail;
    private JavaMailSender mailSender;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:3025}")
    private int mailPort;

    public DevMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing GreenMail for dev profile on {}:{}", mailHost, mailPort);
        ServerSetup serverSetup = new ServerSetup(mailPort, mailHost, ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(serverSetup);
        greenMail.setUser("dev@lebvest.local", "dev@lebvest.local", "devpassword");
        greenMail.start();
        log.info("GreenMail started successfully. Emails will be captured locally.");
        log.info("Access GreenMail via: http://localhost:{}/api/dev/mail (if you add an endpoint)", mailPort);
    }

    @PreDestroy
    public void destroy() {
        if (greenMail != null) {
            greenMail.stop();
            log.info("GreenMail stopped");
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendSimpleMail(String to, String subject, String text) {
        log.info(" [DEV] Sending simple email to: {}, subject: {}", to, subject);
        log.info(" [DEV] Email content: {}", text);
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
            log.info(" [DEV] Email sent successfully to: {} (captured by GreenMail)", to);
        } catch (MessagingException e) {
            log.error(" [DEV] Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendHtmlMail(String to, String subject, String htmlContent, Attachment... attachments) {
        log.info(" [DEV] Sending HTML email to: {}, subject: {}", to, subject);
        log.info(" [DEV] Email HTML preview (first 200 chars): {}", 
                htmlContent.length() > 200 ? htmlContent.substring(0, 200) + "..." : htmlContent);
        
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
                log.info("📎 [DEV] Attachment added: {}", a.filename());
            }

            mailSender.send(message);
            log.info(" [DEV] HTML email sent successfully to: {} (captured by GreenMail)", to);
        } catch (MessagingException e) {
            log.error(" [DEV] Failed to send HTML email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email with attachments", e);
        } catch (Exception e) {
            log.error(" [DEV] Unexpected error sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public String loadAndFormatEmailTemplate(Map<String, String> placeholders, String fileName) {
        try {
            var resource = new ClassPathResource("/static/" + fileName + ".html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            return template;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email template: " + fileName, e);
        }
    }

    /**
     * Get GreenMail instance for accessing captured emails (useful for testing/debugging)
     */
    public GreenMail getGreenMail() {
        return greenMail;
    }
}

