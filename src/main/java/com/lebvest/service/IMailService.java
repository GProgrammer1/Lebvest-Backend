package com.lebvest.service;

import com.lebvest.model.dto.Attachment;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;


public interface IMailService {
    @Async("taskExecutor")
    void sendSimpleMail(String to, String subject, String text);

    @Async("taskExecutor")
    void sendHtmlMail(String to, String subject, String htmlContent, Attachment... attachments);

    String loadAndFormatEmailTemplate(Map<String, String> placeholders, String fileName);
}

