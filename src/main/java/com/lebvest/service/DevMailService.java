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
        log.info("========================================");
        log.info("Initializing GreenMail for dev profile");
        log.info("========================================");
        log.info("Host: {}, Port: {}", mailHost, mailPort);
        
        ServerSetup serverSetup = new ServerSetup(mailPort, mailHost, ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(serverSetup);
        
        // Configure GreenMail to accept emails with or without authentication
        // Set up a user that matches common dev credentials (if JavaMailSender tries to auth)
        greenMail.setUser("dev@lebvest.local", "dev@lebvest.local", "devpassword");
        // Also allow any email address (GreenMail accepts all by default)
        
        greenMail.start();
        
        log.info("✓ GreenMail started successfully");
        log.info("✓ GreenMail is listening on {}:{}", mailHost, mailPort);
        log.info("✓ GreenMail configured to accept emails (with or without auth)");
        log.info("✓ GreenMail will capture ALL emails sent via JavaMailSender");
        log.info("✓ Access captured emails via: http://localhost:8080/api/dev/mail");
        log.info("========================================");
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
        log.info(" [DEV] ===== Sending simple email =====");
        log.info(" [DEV] To: {}", to);
        log.info(" [DEV] Subject: {}", subject);
        log.info(" [DEV] Content: {}", text);
        log.info(" [DEV] GreenMail status: {}", greenMail != null ? "Running" : "NOT INITIALIZED");
        
        if (greenMail == null) {
            log.error(" [DEV] GreenMail is not initialized! Cannot send email.");
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            
            log.info(" [DEV] Attempting to send email via JavaMailSender...");
            mailSender.send(message);
            
            // Wait a bit for GreenMail to process the email
            Thread.sleep(100);
            
            int receivedCount = greenMail.getReceivedMessages().length;
            log.info(" [DEV] ✓ Email sent successfully! Total emails in GreenMail: {}", receivedCount);
            log.info(" [DEV] ===== Email sending completed =====");
        } catch (MessagingException e) {
            log.error(" [DEV] ✗ Failed to send email: {}", e.getMessage(), e);
            log.error(" [DEV] Exception details: ", e);
            throw new RuntimeException("Failed to send email", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(" [DEV] Thread interrupted while waiting for GreenMail", e);
        } catch (Exception e) {
            log.error(" [DEV] ✗ Unexpected error sending email: {}", e.getMessage(), e);
            log.error(" [DEV] Exception details: ", e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendHtmlMail(String to, String subject, String htmlContent, Attachment... attachments) {
        log.info(" [DEV] ===== Sending HTML email =====");
        log.info(" [DEV] To: {}", to);
        log.info(" [DEV] Subject: {}", subject);
        log.info(" [DEV] HTML preview (first 200 chars): {}", 
                htmlContent.length() > 200 ? htmlContent.substring(0, 200) + "..." : htmlContent);
        log.info(" [DEV] Attachments: {}", attachments != null ? attachments.length : 0);
        log.info(" [DEV] GreenMail status: {}", greenMail != null ? "Running" : "NOT INITIALIZED");
        
        if (greenMail == null) {
            log.error(" [DEV] GreenMail is not initialized! Cannot send email.");
            return;
        }
        
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

            log.info(" [DEV] Attempting to send HTML email via JavaMailSender...");
            mailSender.send(message);
            
            // Wait a bit for GreenMail to process the email
            Thread.sleep(100);
            
            int receivedCount = greenMail.getReceivedMessages().length;
            log.info(" [DEV] ✓ HTML email sent successfully! Total emails in GreenMail: {}", receivedCount);
            log.info(" [DEV] ===== HTML email sending completed =====");
        } catch (MessagingException e) {
            log.error(" [DEV] ✗ Failed to send HTML email: {}", e.getMessage(), e);
            log.error(" [DEV] Exception details: ", e);
            throw new RuntimeException("Failed to send email with attachments", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(" [DEV] Thread interrupted while waiting for GreenMail", e);
        } catch (Exception e) {
            log.error(" [DEV] ✗ Unexpected error sending HTML email: {}", e.getMessage(), e);
            log.error(" [DEV] Exception details: ", e);
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

