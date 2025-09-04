package com.lebvest.model.events;


import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.Attachment;
import com.lebvest.model.dto.CompanySignupEmailEvent;
import com.lebvest.model.dto.CompanySignupUploadEvent;
import com.lebvest.service.MailService;
import com.lebvest.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompanySignupWorker {

    private S3Service s3Service;
    private MailService mailService;
    private VarsConfig varsConfig;

    public CompanySignupWorker(S3Service s3Service, MailService mailService, VarsConfig varsConfig) {
        this.s3Service = s3Service;
        this.mailService = mailService;
        this.varsConfig = varsConfig;
    }

    @RabbitListener(queues = "company.signup.upload")
    public void handleUpload(CompanySignupUploadEvent event) {
        s3Service.uploadPendingDocs(event.requestId(), event.files());
    }

    @RabbitListener(queues = "company.signup.email")
    public void handleEmail(CompanySignupEmailEvent event) {
        String html = mailService.loadAndFormatEmailTemplate(event.templateData(), "CompanyRegistrationEmail");
        mailService.sendHtmlMail(varsConfig.getAdminEmail(), event.subject(), html, event.attachments().toArray(new Attachment[0]));
    }
}

