package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.InvestorRegistrationRequest;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorPreference;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.enums.AdminNotificationType;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class InvestorRegistrationService {

    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final IFileStorageService fileStorageService;
    private final IMailService mailService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final AdminNotificationRepository adminNotificationRepository;

    public InvestorRegistrationService(InvestorRepository investorRepository,
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            IFileStorageService fileStorageService,
            AdminNotificationRepository adminNotificationRepository,
            IMailService mailService,
            WebSocketNotificationService webSocketNotificationService) {
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.adminNotificationRepository = adminNotificationRepository;
        this.mailService = mailService;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @Transactional
    public String registerInvestor(InvestorRegistrationRequest investorRegistrationRequest) {
        // Validate that at least one investment category is selected
        if (investorRegistrationRequest.getInvestmentCategories() == null ||
                investorRegistrationRequest.getInvestmentCategories().isEmpty()) {
            throw new IllegalArgumentException("At least one investment preference must be selected");
        }

        // Validate that at least one location is selected
        if (investorRegistrationRequest.getLocations() == null ||
                investorRegistrationRequest.getLocations().isEmpty()) {
            throw new IllegalArgumentException("At least one location preference must be selected");
        }

        // Validate that exactly one risk level is selected
        if (investorRegistrationRequest.getRiskLevels() == null ||
                investorRegistrationRequest.getRiskLevels().isEmpty()) {
            throw new IllegalArgumentException("Risk tolerance must be selected");
        }
        if (investorRegistrationRequest.getRiskLevels().size() > 1) {
            throw new IllegalArgumentException("Only one risk tolerance level can be selected");
        }

        var user = userRepository.findByEmail(investorRegistrationRequest.getEmail()).orElse(null);
        var investorPreferences = InvestorPreference
                .builder()
                .locations(investorRegistrationRequest.getLocations())
                .categories(investorRegistrationRequest.getInvestmentCategories())
                .riskLevels(investorRegistrationRequest.getRiskLevels())
                .build();
        if (user != null) {
            if (investorRepository.existsByUser(user)) {
                throw new ConflictException("Investor profile already exists for this user");
            }
            Set<Role> newRoles = new HashSet<>(user.getRoles());
            newRoles.add(Role.INVESTOR);
            user.setRoles(newRoles);
            userRepository.save(user);
        } else {
            user = User
                    .builder()
                    .name(investorRegistrationRequest.getName())
                    .email(investorRegistrationRequest.getEmail())
                    .password(passwordEncoder.encode(investorRegistrationRequest.getPassword()))
                    .roles(Set.of(Role.INVESTOR))
                    .build();

            userRepository.save(user);

        }
        var investor = Investor.builder()
                .user(user)
                .bio(investorRegistrationRequest.getBio())
                .preferences(investorPreferences)
                .kycStatus(com.lebvest.model.enums.VerificationStatus.PENDING)
                .kycVerified(false)
                .build();
        investorPreferences.setInvestor(investor);

        // Handle document uploads
        if (investorRegistrationRequest.getIdentityDoc() == null
                || investorRegistrationRequest.getIdentityDoc().isEmpty()) {
            throw new IllegalArgumentException("Identity document is required");
        }
        if (investorRegistrationRequest.getAddressDoc() == null
                || investorRegistrationRequest.getAddressDoc().isEmpty()) {
            throw new IllegalArgumentException("Address proof is required");
        }
        if (investorRegistrationRequest.getSelfieDoc() == null
                || investorRegistrationRequest.getSelfieDoc().isEmpty()) {
            throw new IllegalArgumentException("Selfie is required");
        }
        if (investorRegistrationRequest.getSourceOfFundsDoc() == null
                || investorRegistrationRequest.getSourceOfFundsDoc().isEmpty()) {
            throw new IllegalArgumentException("Source of funds document is required");
        }

        try {
            if (investorRegistrationRequest.getIdentityDoc() != null) {
                String path = fileStorageService.uploadFile("investor/identity",
                        investorRegistrationRequest.getIdentityDoc().getOriginalFilename(),
                        investorRegistrationRequest.getIdentityDoc().getInputStream(),
                        investorRegistrationRequest.getIdentityDoc().getSize(),
                        investorRegistrationRequest.getIdentityDoc().getContentType());
                investor.setIdentityDocUrl(path);
            }
            if (investorRegistrationRequest.getAddressDoc() != null) {
                String path = fileStorageService.uploadFile("investor/address",
                        investorRegistrationRequest.getAddressDoc().getOriginalFilename(),
                        investorRegistrationRequest.getAddressDoc().getInputStream(),
                        investorRegistrationRequest.getAddressDoc().getSize(),
                        investorRegistrationRequest.getAddressDoc().getContentType());
                investor.setAddressDocUrl(path);
            }
            if (investorRegistrationRequest.getSelfieDoc() != null) {
                String path = fileStorageService.uploadFile("investor/selfie",
                        investorRegistrationRequest.getSelfieDoc().getOriginalFilename(),
                        investorRegistrationRequest.getSelfieDoc().getInputStream(),
                        investorRegistrationRequest.getSelfieDoc().getSize(),
                        investorRegistrationRequest.getSelfieDoc().getContentType());
                investor.setSelfieDocUrl(path);
            }
            if (investorRegistrationRequest.getSourceOfFundsDoc() != null) {
                String path = fileStorageService.uploadFile("investor/source-of-funds",
                        investorRegistrationRequest.getSourceOfFundsDoc().getOriginalFilename(),
                        investorRegistrationRequest.getSourceOfFundsDoc().getInputStream(),
                        investorRegistrationRequest.getSourceOfFundsDoc().getSize(),
                        investorRegistrationRequest.getSourceOfFundsDoc().getContentType());
                investor.setSourceOfFundsDocUrl(path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload verification documents", e);
        }

        investorRepository.save(investor);

        // Save investor ID for use after transaction commit
        Long investorId = investor.getId();

        // Notify Admins - create notifications synchronously (within transaction)
        createAdminNotifications(investor);

        // Send emails AFTER transaction commits to avoid race condition
        // The async email thread needs the investor data to be fully committed
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.info(">>> Sending admin notification emails (after commit) for investor ID: {} <<<", investorId);
                        // Reload investor to ensure we have the latest data after commit
                        Investor reloadedInvestor = investorRepository.findById(investorId)
                                .orElseThrow(() -> new IllegalArgumentException("Investor not found: " + investorId));
                        
                        sendAdminNotificationEmails(reloadedInvestor);
                        log.info("✓ Admin notification emails sent successfully for investor: {}", reloadedInvestor.getUser().getName());
                    } catch (Exception e) {
                        log.error("✗✗✗ FAILED to send admin notification emails for investor ID: {} - {} ✗✗✗", 
                                investorId, e.getMessage(), e);
                        // Continue - notification should still be saved in DB
                    }
                }
            }
        );

        return jwtService.generateToken(user, "access", user.getId());
    }

    private void createAdminNotifications(Investor investor) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.ADMIN))
                .toList();

        for (User admin : admins) {
            AdminNotification notification = AdminNotification.builder()
                    .admin(admin)
                    .investor(investor)
                    .type(AdminNotificationType.VERIFICATION_REQUEST)
                    .title("Investor Verification")
                    .message("New investor verification request from " + investor.getUser().getName())
                    .read(false)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            adminNotificationRepository.save(notification);

            // Notify admin via WebSocket (this is synchronous, so it's fine)
            webSocketNotificationService.notifyAdmin(admin.getId(), notification);
        }
    }

    private void sendAdminNotificationEmails(Investor investor) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.ADMIN))
                .toList();

        for (User admin : admins) {
            // Send email to admin - this is async and will run after transaction commit
            mailService.sendSimpleMail(
                    admin.getEmail(),
                    "New Investor Registration: " + investor.getUser().getName(),
                    "A new investor has registered and uploaded verification documents.\n" +
                            "Name: " + investor.getUser().getName() + "\n" +
                            "Email: " + investor.getUser().getEmail() + "\n" +
                            "Please review the documents in the admin dashboard.");
        }
    }
}
