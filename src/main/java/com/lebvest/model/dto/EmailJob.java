package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailJob {
    private String to;
    private String subject;
    private String templateName;
    private Map<String, String> templateData;
    private List<Attachment> attachments;
}

