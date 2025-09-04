package com.lebvest.model.events;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Generic templated email send request.
 * If toEmail is null, listener will route to the configured admin email.
 * attachments is optional.
 */
public class CompanySignupEmailEvent implements Serializable {
    private String subject;
    private String templateName;
    private Map<String, String> templateData;
    private List<Attachment> attachments;
    private String toEmail;

    public CompanySignupEmailEvent() {}

    public CompanySignupEmailEvent(
            String subject,
            String templateName,
            Map<String, String> templateData,
            List<Attachment> attachments,
            String toEmail
    ) {
        this.subject = subject;
        this.templateName = templateName;
        this.templateData = templateData;
        this.attachments = attachments;
        this.toEmail = toEmail;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Map<String, String> getTemplateData() { return templateData; }
    public void setTemplateData(Map<String, String> templateData) { this.templateData = templateData; }

    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }
}
