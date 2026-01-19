package com.lebvest.controller;

import com.lebvest.service.DevMailService;
import com.lebvest.service.IMailService;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dev/mail")
@Profile("dev")
public class DevMailController {

    private final DevMailService devMailService;

    @Autowired
    public DevMailController(@Qualifier("devMailService") IMailService mailService) {
        // Unwrap the proxy to get the actual DevMailService instance
        try {
            if (mailService instanceof Advised) {
                Object target = ((Advised) mailService).getTargetSource().getTarget();
                if (target == null) {
                    throw new IllegalStateException("Target object is null");
                }
                this.devMailService = (DevMailService) target;
            } else if (mailService instanceof DevMailService) {
                // If it's already the concrete class (CGLib proxy)
                this.devMailService = (DevMailService) mailService;
            } else {
                throw new IllegalStateException("Cannot unwrap proxy to DevMailService. Got: " + mailService.getClass());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unwrap DevMailService proxy: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMails() {
        log.debug("DevMailController: Getting all emails from GreenMail");
        
        if (devMailService == null || devMailService.getGreenMail() == null) {
            log.error("DevMailController: DevMailService or GreenMail is null!");
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<Map<String, Object>> mails = new ArrayList<>();
        MimeMessage[] messages = devMailService.getGreenMail().getReceivedMessages();
        
        log.debug("DevMailController: Found {} messages in GreenMail", messages.length);

        // iterate in reverse order to show latest first
        for (int i = messages.length - 1; i >= 0; i--) {
            MimeMessage msg = messages[i];
            try {
                Map<String, Object> mail = new HashMap<>();
                mail.put("id", i);
                mail.put("subject", msg.getSubject());
                mail.put("from",
                        msg.getFrom() != null && msg.getFrom().length > 0 ? msg.getFrom()[0].toString() : "Unknown");

                // Handle recipients safely
                try {
                    mail.put("to",
                            msg.getAllRecipients() != null && msg.getAllRecipients().length > 0
                                    ? msg.getAllRecipients()[0].toString()
                                    : "Unknown");
                } catch (Exception e) {
                    mail.put("to", "Unknown");
                }

                mail.put("sentDate", msg.getSentDate());

                Object content = msg.getContent();
                if (content instanceof String) {
                    mail.put("body", content);
                } else if (content instanceof MimeMultipart) {
                    mail.put("body", "[Multipart Content - See Logs for details or expand viewer]");
                } else {
                    mail.put("body", content.toString());
                }

                mails.add(mail);
            } catch (Exception e) {
                // Skip malformed messages
            }
        }
        return ResponseEntity.ok(mails);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearMails() {
        log.debug("DevMailController: Clearing all emails from GreenMail");
        if (devMailService != null && devMailService.getGreenMail() != null) {
            devMailService.getGreenMail().reset();
            log.debug("DevMailController: All emails cleared");
        }
        return ResponseEntity.noContent().build();
    }
}
