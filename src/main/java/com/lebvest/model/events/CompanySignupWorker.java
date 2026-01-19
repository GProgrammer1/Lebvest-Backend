package com.lebvest.model.events;


import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.Attachment;
import com.lebvest.model.dto.CompanySignupEmailEvent;
import com.lebvest.model.dto.CompanySignupUploadEvent;
import com.lebvest.service.IMailService;
import com.lebvest.service.IFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
//@Component  // Disabled - RabbitMQ not needed
public class CompanySignupWorker {

    private IFileStorageService fileStorageService;
    private IMailService mailService;
    private VarsConfig varsConfig;

    public CompanySignupWorker(IFileStorageService fileStorageService, IMailService mailService, VarsConfig varsConfig) {
        this.fileStorageService = fileStorageService;
        this.mailService = mailService;
        this.varsConfig = varsConfig;
    }

    @RabbitListener(queues = "company.signup.upload")
    public void handleUpload(CompanySignupUploadEvent event) {
        fileStorageService.uploadPendingDocs(event.requestId(), event.files());
    }

    @RabbitListener(queues = "company.signup.email")
    public void handleEmail(CompanySignupEmailEvent event) {
        String html = mailService.loadAndFormatEmailTemplate(event.templateData(), "CompanyRegistrationEmail");
        mailService.sendHtmlMail(varsConfig.getAdminEmail(), event.subject(), html, event.attachments().toArray(new Attachment[0]));
    }
}

