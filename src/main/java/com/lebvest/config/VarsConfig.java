package com.lebvest.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Getter
@Configuration
public class VarsConfig {

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${aws.s3.bucket}")
    private String awsBucket;

    @Value("${url.pending-prefix}")
    private String pendingDocsPrefix;

    @Value("${url.accepted-prefix}")
    private String acceptedDocsPrefix;

    @Value("${queue.company.signup.upload}")
    private String signupCompanyUploadQueueName;

    @Value("${queue.company.signup.email}")
    private String signupCompanyEmailQueueName;

    @Value("${queue.company.signup.accepted.move}")
    private String signupCompanyAcceptedMoveQueueName;

    /**
     * Builds the full pending path prefix for a specific signup request.
     */
    public String getPendingPrefix(UUID uuid) {
        return pendingDocsPrefix + uuid;
    }

    /**
     * Builds the full accepted path prefix for a specific signup request.
     */
    public String getAcceptedPrefix(UUID uuid) {
        return acceptedDocsPrefix + uuid;
    }

    /**
     * Returns the full reset-password link for emails.
     */
    public String getResetLink(String token) {
        return frontendUrl + "/reset-password/" + token;
    }
}
