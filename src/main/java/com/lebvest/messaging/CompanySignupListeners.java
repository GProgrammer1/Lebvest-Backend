package com.lebvest.messaging;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.events.Attachment;
import com.lebvest.model.events.CompanySignupAcceptedMoveEvent;
import com.lebvest.model.events.CompanySignupEmailEvent;
import com.lebvest.model.events.CompanySignupUploadEvent;
import com.lebvest.service.MailService;
import com.lebvest.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

//@Service  // Disabled - RabbitMQ not needed
public class CompanySignupListeners {

    private static final Logger log = LoggerFactory.getLogger(CompanySignupListeners.class);

    private final VarsConfig varsConfig;
    private final S3Service s3Service;
    private final MailService mailService;

    public CompanySignupListeners(VarsConfig varsConfig, S3Service s3Service, MailService mailService) {
        this.varsConfig = varsConfig;
        this.s3Service = s3Service;
        this.mailService = mailService;
    }

    @RabbitListener(queues = "company.signup.upload")
    public void handleUpload(CompanySignupUploadEvent event) {
        var prefix = varsConfig.getPendingPrefix(event.getRequestId());
        log.info("Uploading {} signup docs to S3 prefix {}",
                event.getFiles() != null ? event.getFiles().size() : 0, prefix);

        if (event.getFiles() == null || event.getFiles().isEmpty()) {
            log.warn("Upload event had no files. requestId={}", event.getRequestId());
            return;
        }

        for (Attachment a : event.getFiles()) {
            try (var in = new ByteArrayInputStream(a.getContent())) {
                s3Service.uploadFile(
                        prefix,
                        a.getFilename(),
                        in,
                        a.getContent() == null ? 0L : a.getContent().length,
                        a.getContentType()
                );
            } catch (Exception ex) {
                log.error("Failed uploading {} for requestId={}: {}", a.getFilename(), event.getRequestId(), ex.getMessage());
            }
        }
        log.info("Upload complete for requestId={}", event.getRequestId());
    }

    @RabbitListener(queues = "company.signup.email")
    public void handleEmail(CompanySignupEmailEvent event) {
        try {
            var html = mailService.loadAndFormatEmailTemplate(event.getTemplateData(), event.getTemplateName());
            var to = event.getToEmail() != null ? event.getToEmail() : varsConfig.getAdminEmail();

            if (event.getAttachments() != null && !event.getAttachments().isEmpty()) {
                var arr = event.getAttachments()
                        .stream()
                        .map(a -> new com.lebvest.model.dto.Attachment(a.getFilename(), a.getContent(), a.getContentType()))
                        .toArray(com.lebvest.model.dto.Attachment[]::new);
                mailService.sendHtmlMail(to, event.getSubject(), html, arr);
            } else {
                mailService.sendHtmlMail(to, event.getSubject(), html);
            }
            log.info("Email queued->sent to {} with template {}", to, event.getTemplateName());
        } catch (Exception ex) {
            log.error("Email send failed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = "company.signup.accepted.move")
    public void handleAcceptedMove(CompanySignupAcceptedMoveEvent event) {
        var requestId = event.getRequestId();
        var pendingPrefix = varsConfig.getPendingPrefix(requestId);
        var acceptedPrefix = varsConfig.getAcceptedPrefix(requestId);

        List<String> keys = event.getKeys();
        if (keys == null || keys.isEmpty()) {
            keys = s3Service.listFilesByPrefix(pendingPrefix);
        }

        if (keys == null || keys.isEmpty()) {
            log.info("No pending documents to move for requestId={}", requestId);
            return;
        }

        try {
            s3Service.moveFilesAndDelete(keys, acceptedPrefix);
            log.info("Moved {} document(s) from {} to {} for requestId={}",
                    keys.size(), pendingPrefix, acceptedPrefix, requestId);
        } catch (Exception ex) {
            log.error("Failed moving documents for requestId={}: {}", requestId, ex.getMessage(), ex);
            // consider retry / DLQ policy if you need stronger guarantees
        }
    }
}
