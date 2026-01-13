package com.lebvest.util;

import com.lebvest.model.dto.AdminNotificationDto;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.enums.AdminNotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminNotificationMapper implements GenericMapper<AdminNotification, AdminNotificationDto> {

    @Value("${frontend.url}")
    private String frontendUrl;

    public AdminNotification toEntity(AdminNotificationDto adminNotificationDto) {
        return AdminNotification.builder()
                .id(adminNotificationDto.getId())
                .type(adminNotificationDto.getType())
                .title(adminNotificationDto.getTitle())
                .message(adminNotificationDto.getMessage())
                .isAccepted(adminNotificationDto.getIsAccepted())
                .read(adminNotificationDto.isRead())
                .createdAt(adminNotificationDto.getCreatedAt())
                .build();
    }

    public AdminNotificationDto toDto(AdminNotification adminNotification) {
        List<String> documentUrls = extractDocumentUrls(adminNotification);

        return AdminNotificationDto
                .builder()
                .id(adminNotification.getId())
                .adminId(adminNotification.getAdmin().getId())
                .reqId(adminNotification.getRequest() != null ? adminNotification.getRequest().getId() : null)
                .companyId(adminNotification.getCompany() != null ? adminNotification.getCompany().getId() : null)
                .title(adminNotification.getTitle())
                .message(adminNotification.getMessage())
                .isAccepted(adminNotification.getIsAccepted())
                .read(adminNotification.isRead())
                .type(adminNotification.getType())
                .createdAt(adminNotification.getCreatedAt())
                .documentUrls(documentUrls)
                .build();
    }

    // Static method for backward compatibility

    private List<String> extractDocumentUrls(AdminNotification notification) {
        List<String> urls = new ArrayList<>();

        if (notification.getType() == AdminNotificationType.SIGNUP_REQUEST) {
            // Extract documents from CompanySignupRequest
            CompanySignupRequest request = notification.getRequest();
            if (request != null && request.getDocuments() != null) {
                urls.addAll(request.getDocuments().stream()
                        .map(this::convertPathToUrl)
                        .collect(Collectors.toList()));
            }
        } else if (notification.getType() == AdminNotificationType.PROJECT_PROPOSAL) {
            // Extract documents from Investment
            Investment investment = notification.getInvestment();
            if (investment != null) {
                urls.addAll(extractInvestmentDocumentUrls(investment));
            }
        } else if (notification.getType() == AdminNotificationType.VERIFICATION_REQUEST) {
            // Extract documents from CompanyVerificationDocuments or Investor
            if (notification.getCompany() != null) {
                // Documents will be added in AdminService.populateDocumentUrls
            } else if (notification.getInvestor() != null) {
                com.lebvest.model.entities.investor.Investor investor = notification.getInvestor();
                addIfNotNull(urls, investor.getIdentityDocUrl());
                addIfNotNull(urls, investor.getAddressDocUrl());
                addIfNotNull(urls, investor.getSelfieDocUrl());
                addIfNotNull(urls, investor.getSourceOfFundsDocUrl());
            }
        }

        return urls;
    }

    /**
     * Extract all document URLs from CompanyVerificationDocuments
     */
    public List<String> extractVerificationDocumentUrls(CompanyVerificationDocuments docs) {
        List<String> urls = new ArrayList<>();

        if (docs == null) {
            return urls;
        }

        // Single document fields
        addIfNotNull(urls, docs.getCertificateOfIncorporation());
        addIfNotNull(urls, docs.getArticlesOfAssociation());
        addIfNotNull(urls, docs.getTaxRegistrationCertificate());
        addIfNotNull(urls, docs.getProofOfRegisteredAddress());
        addIfNotNull(urls, docs.getShareholderStructure());
        addIfNotNull(urls, docs.getBoardResolution());
        addIfNotNull(urls, docs.getPepSanctionsDeclaration());
        addIfNotNull(urls, docs.getBankAccountConfirmation());
        addIfNotNull(urls, docs.getSourceOfFundsDeclaration());

        // List fields
        if (docs.getUboIds() != null) {
            urls.addAll(docs.getUboIds());
        }
        if (docs.getDirectorIds() != null) {
            urls.addAll(docs.getDirectorIds());
        }
        if (docs.getAuthorizedSignatoryIds() != null) {
            urls.addAll(docs.getAuthorizedSignatoryIds());
        }
        if (docs.getFinancialStatements() != null) {
            urls.addAll(docs.getFinancialStatements());
        }
        if (docs.getManagementAccounts() != null) {
            urls.addAll(docs.getManagementAccounts());
        }
        if (docs.getBankStatements() != null) {
            urls.addAll(docs.getBankStatements());
        }

        // Convert all paths to URLs
        return urls.stream()
                .map(this::convertPathToUrl)
                .collect(Collectors.toList());
    }

    /**
     * Extract document URLs from Investment
     */
    public List<String> extractInvestmentDocumentUrls(Investment investment) {
        List<String> urls = new ArrayList<>();

        if (investment == null || investment.getDocuments() == null) {
            return urls;
        }

        urls.addAll(investment.getDocuments().stream()
                .map(doc -> doc.getUrl())
                .map(this::convertPathToUrl)
                .collect(Collectors.toList()));

        return urls;
    }

    private void addIfNotNull(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value);
        }
    }

    /**
     * Convert file path to accessible URL
     * Assumes files are served via /api/files/{path}
     */
    private String convertPathToUrl(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }

        // If already a full URL, return as is
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }

        // Convert relative path to URL
        // Format: http://localhost:8080/api/files/{path}
        // Use frontend URL and replace port with backend port (same approach as
        // AdminService)
        String baseUrl = frontendUrl.replace(":3000", ":8080");
        // Ensure we use localhost instead of 0.0.0.0 or other binding addresses
        // (0.0.0.0 is for server binding, not client URLs)
        baseUrl = baseUrl.replace("0.0.0.0", "localhost");
        // If frontend URL doesn't contain :8080, default to localhost:8080
        if (!baseUrl.contains(":8080")) {
            baseUrl = "http://localhost:8080";
        }
        return baseUrl + "/api/files/" + filePath.replace("\\", "/");
    }
}
