package com.lebvest.model.dto;

import java.util.List;
import java.util.Map;

public record CompanySignupEmailEvent(
        String subject,
        Map<String, String> templateData,
        List<Attachment> attachments
) {}
