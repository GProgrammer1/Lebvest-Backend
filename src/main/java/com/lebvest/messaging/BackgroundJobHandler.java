package com.lebvest.messaging;

import com.lebvest.model.dto.DocumentProcessingJob;
import com.lebvest.model.dto.EmailJob;
import com.lebvest.model.dto.PayoutCalculationJob;
import com.lebvest.service.MailService;
import com.lebvest.service.StripeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BackgroundJobHandler {

    private final MailService mailService;
    private final StripeService stripeService;

    public BackgroundJobHandler(MailService mailService, StripeService stripeService) {
        this.mailService = mailService;
        this.stripeService = stripeService;
    }

    @RabbitListener(queues = "email.queue")
    public void handleEmailJob(EmailJob job) {
        try {
            log.info("Processing email job: {} to {}", job.getTemplateName(), job.getTo());
            
            String htmlContent = mailService.loadAndFormatEmailTemplate(
                    job.getTemplateData(),
                    job.getTemplateName()
            );

            if (job.getAttachments() != null && !job.getAttachments().isEmpty()) {
                mailService.sendHtmlMail(
                        job.getTo(),
                        job.getSubject(),
                        htmlContent,
                        job.getAttachments().toArray(new com.lebvest.model.dto.Attachment[0])
                );
            } else {
                mailService.sendHtmlMail(job.getTo(), job.getSubject(), htmlContent);
            }

            log.info("Email job completed successfully: {}", job.getTo());
        } catch (Exception e) {
            log.error("Failed to process email job: {}", e.getMessage(), e);
            throw new RuntimeException("Email job failed", e);
        }
    }

    @RabbitListener(queues = "document.processing.queue")
    public void handleDocumentProcessingJob(DocumentProcessingJob job) {
        try {
            log.info("Processing document job: {} for company/investment {}", job.getJobType(), 
                    job.getCompanyId() != null ? job.getCompanyId() : job.getInvestmentId());

            // Process documents based on job type
            switch (job.getJobType()) {
                case "UPLOAD":
                    // Documents already uploaded to S3, just update status
                    log.info("Documents uploaded: {}", job.getDocumentKeys());
                    break;
                case "PROCESS":
                    // Process documents (e.g., extract text, generate thumbnails)
                    log.info("Processing documents: {}", job.getDocumentKeys());
                    break;
                case "VALIDATE":
                    // Validate document content
                    log.info("Validating documents: {}", job.getDocumentKeys());
                    break;
                default:
                    log.warn("Unknown document job type: {}", job.getJobType());
            }

            log.info("Document processing job completed: {}", job.getJobType());
        } catch (Exception e) {
            log.error("Failed to process document job: {}", e.getMessage(), e);
            throw new RuntimeException("Document processing job failed", e);
        }
    }

    @RabbitListener(queues = "payout.calculation.queue")
    public void handlePayoutCalculationJob(PayoutCalculationJob job) {
        try {
            log.info("Processing payout calculation job: {} for investment {}", 
                    job.getCalculationType(), job.getInvestmentId());

            // Calculate payout based on type
            switch (job.getCalculationType()) {
                case "MONTHLY":
                    // Calculate monthly payout
                    log.info("Calculating monthly payout for investment {}", job.getInvestmentId());
                    break;
                case "QUARTERLY":
                    // Calculate quarterly payout
                    log.info("Calculating quarterly payout for investment {}", job.getInvestmentId());
                    break;
                case "ANNUAL":
                    // Calculate annual payout
                    log.info("Calculating annual payout for investment {}", job.getInvestmentId());
                    break;
                case "FINAL":
                    // Calculate final payout
                    log.info("Calculating final payout for investment {}", job.getInvestmentId());
                    break;
                default:
                    log.warn("Unknown payout calculation type: {}", job.getCalculationType());
            }

            log.info("Payout calculation job completed: {}", job.getCalculationType());
        } catch (Exception e) {
            log.error("Failed to process payout calculation job: {}", e.getMessage(), e);
            throw new RuntimeException("Payout calculation job failed", e);
        }
    }
}

