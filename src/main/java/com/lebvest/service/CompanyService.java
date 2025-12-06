package com.lebvest.service;

import com.lebvest.exception.ResourceNotFoundException;
import com.lebvest.model.dto.*;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyFinancial;
import com.lebvest.model.entities.company.CompanySocialMedia;
import com.lebvest.model.entities.company.CompanyTeamMember;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestmentHighlight;
import com.lebvest.model.entities.investment.InvestmentUpdate;
import com.lebvest.model.entities.investment.InvestorInvestment;
import com.lebvest.model.entities.investor.User;
import org.springframework.web.multipart.MultipartFile;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanyVerificationDocumentsRepository;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestmentRequestRepository;
import com.lebvest.repository.InvestmentUpdateRepository;
import com.lebvest.repository.InvestorInvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import com.lebvest.model.enums.CompanyStatus;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentStatus;
import com.lebvest.model.enums.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.Location;
import com.lebvest.config.VarsConfig;
import com.lebvest.exception.BadRequestException;
import com.lebvest.model.dto.investor.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lebvest.model.dto.InvestmentRequestDto;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final CompanyVerificationDocumentsRepository verificationDocumentsRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentRequestRepository investmentRequestRepository;
    private final InvestmentUpdateRepository investmentUpdateRepository;
    private final InvestorInvestmentRepository investorInvestmentRepository;
    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final InvestmentService investmentService;
    private final com.lebvest.controller.AdminNotificationSseController adminNotificationSseController;
    private final MailService mailService;
    private final VarsConfig varsConfig;
    private final PasswordEncoder passwordEncoder;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanyVerificationDocumentsRepository verificationDocumentsRepository,
            InvestmentRepository investmentRepository,
            InvestmentRequestRepository investmentRequestRepository,
            InvestmentUpdateRepository investmentUpdateRepository,
            InvestorInvestmentRepository investorInvestmentRepository,
            InvestorRepository investorRepository,
            UserRepository userRepository,
            InvestmentService investmentService,
            com.lebvest.controller.AdminNotificationSseController adminNotificationSseController,
            MailService mailService,
            VarsConfig varsConfig,
            PasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.verificationDocumentsRepository = verificationDocumentsRepository;
        this.investmentRepository = investmentRepository;
        this.investmentRequestRepository = investmentRequestRepository;
        this.investmentUpdateRepository = investmentUpdateRepository;
        this.investorInvestmentRepository = investorInvestmentRepository;
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
        this.investmentService = investmentService;
        this.adminNotificationSseController = adminNotificationSseController;
        this.mailService = mailService;
        this.varsConfig = varsConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public InvestmentDto createInvestment(CreateInvestmentRequest request) {
        Company company = getCurrentCompany();
        
        // Check if company is fully verified and can post projects
        if (!company.getStatus().canPostProjects()) {
            throw new IllegalStateException("Company must be fully verified to post projects. Please complete the verification process.");
        }
        
        Investment investment = Investment.builder()
                .company(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .riskLevel(request.getRiskLevel())
                .expectedReturn(request.getExpectedReturn())
                .minInvestment(request.getMinInvestment())
                .targetAmount(request.getTargetAmount())
                .raisedAmount(BigDecimal.ZERO)
                .location(request.getLocation())
                .investmentType(request.getInvestmentType())
                .durationMonths(request.getDurationMonths())
                .deadline(request.getDeadline())
                .imageUrl(request.getImageUrl())
                .fundingStage(request.getFundingStage())
                .status(InvestmentStatus.PENDING_REVIEW) // New investments require admin approval
                .build();

        Investment savedInvestment = investmentRepository.save(investment);

        // Notify admins about new project posting
        adminNotificationSseController.notifyAllAdminsProject(savedInvestment);

        // Add highlights if provided
        if (request.getHighlights() != null && !request.getHighlights().isEmpty()) {
            final Investment finalInvestment = savedInvestment;
            List<InvestmentHighlight> highlights = request.getHighlights().stream()
                    .map(highlight -> InvestmentHighlight.builder()
                            .investment(finalInvestment)
                            .highlight(highlight)
                            .build())
                    .collect(Collectors.toList());
            savedInvestment.setHighlights(highlights);
            savedInvestment = investmentRepository.save(savedInvestment);
        }

        return convertToDto(savedInvestment);
    }

    @Transactional
    public InvestmentDto updateInvestment(Long investmentId, UpdateInvestmentRequest request) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to update this investment");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            investment.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            investment.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            investment.setCategory(request.getCategory());
        }
        if (request.getRiskLevel() != null) {
            investment.setRiskLevel(request.getRiskLevel());
        }
        if (request.getExpectedReturn() != null) {
            investment.setExpectedReturn(request.getExpectedReturn());
        }
        if (request.getMinInvestment() != null) {
            investment.setMinInvestment(request.getMinInvestment());
        }
        if (request.getTargetAmount() != null) {
            investment.setTargetAmount(request.getTargetAmount());
        }
        if (request.getLocation() != null) {
            investment.setLocation(request.getLocation());
        }
        if (request.getInvestmentType() != null) {
            investment.setInvestmentType(request.getInvestmentType());
        }
        if (request.getDurationMonths() != null) {
            investment.setDurationMonths(request.getDurationMonths());
        }
        if (request.getDeadline() != null) {
            investment.setDeadline(request.getDeadline());
        }
        if (request.getImageUrl() != null) {
            investment.setImageUrl(request.getImageUrl());
        }
        if (request.getFundingStage() != null) {
            investment.setFundingStage(request.getFundingStage());
        }

        // Update highlights if provided
        if (request.getHighlights() != null) {
            // Get or initialize the highlights collection
            List<InvestmentHighlight> highlightsList = investment.getHighlights();
            if (highlightsList == null) {
                highlightsList = new ArrayList<>();
                investment.setHighlights(highlightsList);
            }
            
            // Clear existing highlights (this triggers orphan deletion)
            highlightsList.clear();
            
            // Add new highlights to the same collection (don't replace it)
            final List<InvestmentHighlight> finalHighlightsList = highlightsList;
            request.getHighlights().forEach(highlightText -> {
                InvestmentHighlight highlight = InvestmentHighlight.builder()
                        .investment(investment)
                        .highlight(highlightText)
                        .build();
                finalHighlightsList.add(highlight);
            });
        }

        Investment savedInvestment = investmentRepository.save(investment);
        return convertToDto(savedInvestment);
    }

    @Transactional(readOnly = true)
    public InvestmentDetailDto getInvestmentDetail(Long investmentId) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to view this investment");
        }

        // Get investor investments
        List<InvestorInvestment> investorInvestments = investorInvestmentRepository.findByInvestment(investment);

        // Convert to DTOs
        List<InvestmentDetailDto.InvestorInfoDto> investors = investorInvestments.stream()
                .map(ii -> InvestmentDetailDto.InvestorInfoDto.builder()
                        .investorId(ii.getInvestor().getId())
                        .investorName(ii.getInvestor().getUser().getName())
                        .investorEmail(ii.getInvestor().getUser().getEmail())
                        .investedAmount(ii.getAmount())
                        .currentValue(ii.getCurrentValue())
                        .investedAt(ii.getInvestedAt())
                        .build())
                .collect(Collectors.toList());

        InvestmentDto investmentDto = convertToDto(investment);

        return InvestmentDetailDto.builder()
                .investment(investmentDto)
                .investors(investors)
                .build();
    }

    @Transactional
    public InvestmentDto.UpdateDto createInvestmentUpdate(Long investmentId, CreateInvestmentUpdateRequest request) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to create updates for this investment");
        }

        InvestmentUpdate update = new InvestmentUpdate();
        update.setInvestment(investment);
        update.setUpdateDate(request.getUpdateDate());
        update.setTitle(request.getTitle());
        update.setContent(request.getContent());

        update = investmentUpdateRepository.save(update);

        return InvestmentDto.UpdateDto.builder()
                .date(update.getUpdateDate())
                .title(update.getTitle())
                .content(update.getContent())
                .build();
    }

    private Company getCurrentCompany() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated company");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = companyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found for current user"));

        return company;
    }

    private Company getCurrentCompanyWithRelations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Unable to determine authenticated company");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Fetch company with basic relations (user, socialMedia) - can't fetch multiple bags in one query
        Company company = companyRepository.findByUserWithBasicRelations(user)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found for current user"));

        // Initialize lazy collections by accessing them (Hibernate will fetch them)
        // This is safe because we're in a @Transactional method
        if (company.getTeamMembers() != null) {
            company.getTeamMembers().size(); // Force initialization
        }
        if (company.getFinancials() != null) {
            company.getFinancials().size(); // Force initialization
        }

        return company;
    }

    @Transactional
    public CompanyFinancial addFinancial(AddCompanyFinancialRequest request) {
        Company company = getCurrentCompany();
        
        CompanyFinancial financial = CompanyFinancial.builder()
                .company(company)
                .year(request.getYear())
                .revenue(request.getRevenue())
                .expenses(request.getExpenses())
                .profit(request.getProfit())
                .build();
        
        // Add to company's financials list
        if (company.getFinancials() == null) {
            company.setFinancials(new ArrayList<>());
        }
        company.getFinancials().add(financial);
        
        // Save company (cascade will save financial)
        Company savedCompany = companyRepository.save(company);
        
        // Get the saved financial from the company (it should have an ID now)
        // Since we just added it, it should be the last one in the list
        List<CompanyFinancial> financials = savedCompany.getFinancials();
        if (!financials.isEmpty()) {
            CompanyFinancial savedFinancial = financials.get(financials.size() - 1);
            return savedFinancial;
        }
        
        return financial;
    }

    @Transactional
    public String uploadDocument(MultipartFile file) {
        Company company = getCurrentCompany();
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        
        try {
            // Get project root directory (where the application is running from)
            String projectRoot = System.getProperty("user.dir");
            Path uploadsBasePath = Paths.get(projectRoot, "uploads", "companies", String.valueOf(company.getId()), "documents");
            
            // Create directory structure if it doesn't exist
            try {
                Files.createDirectories(uploadsBasePath);
                log.info("Upload directory: {}", uploadsBasePath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to create upload directory: {}", uploadsBasePath.toAbsolutePath(), e);
                throw new RuntimeException("Failed to create upload directory: " + e.getMessage(), e);
            }
            
            // Generate unique filename to avoid conflicts
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                originalFileName = "file";
            }
            
            // Extract file extension
            String fileExtension = "";
            String baseFileName = originalFileName;
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
                fileExtension = originalFileName.substring(lastDotIndex); // includes the dot
                baseFileName = originalFileName.substring(0, lastDotIndex);
            }
            
            // Sanitize base filename and create unique name
            String sanitizedBaseName = baseFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String uniqueFileName = System.currentTimeMillis() + "_" + sanitizedBaseName + fileExtension;
            
            // Save file locally using absolute path
            Path targetFilePath = uploadsBasePath.resolve(uniqueFileName);
            try {
                Files.copy(file.getInputStream(), targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("File uploaded successfully: {}", targetFilePath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to save file to: {}", targetFilePath.toAbsolutePath(), e);
                throw new RuntimeException("Failed to save file: " + e.getMessage() + ". Please check file permissions and disk space.", e);
            }
            
            // Store relative path for database (e.g., "uploads/companies/1/documents/filename.pdf")
            String relativePath = "uploads/companies/" + company.getId() + "/documents/" + uniqueFileName;
            
            // Note: We don't add to company.documents here because:
            // 1. Verification documents are stored in CompanyVerificationDocuments entity
            // 2. Adding to @ElementCollection causes Hibernate to delete and re-insert all documents
            // 3. This method is used for verification document uploads, not general company documents
            // The URL will be stored in CompanyVerificationDocuments when submitVerificationDocuments is called
            
            return relativePath;
        } catch (RuntimeException e) {
            // Re-throw RuntimeException as-is (already wrapped from inner try-catch blocks)
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading document: {}", e.getMessage(), e);
            log.error("Stack trace:", e);
            throw new RuntimeException("Unexpected error uploading document: " + e.getMessage(), e);
        }
    }

    private InvestmentDto convertToDto(Investment investment) {
        // Use InvestmentService's conversion method
        List<Long> watchlistIds = new ArrayList<>(); // Empty for company view
        return investmentService.convertToDto(investment, watchlistIds);
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto getCompanyProfileById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return convertToProfileDto(company);
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto getCurrentCompanyProfile() {
        Company company = getCurrentCompanyWithRelations();
        return convertToProfileDto(company);
    }

    @Transactional
    public CompanyProfileDto updateCompanyProfile(UpdateCompanyProfileRequest request) {
        Company company = getCurrentCompany();

        // Update basic fields
        if (request.getName() != null) {
            company.setName(request.getName());
        }
        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getLogo() != null) {
            company.setLogo(request.getLogo());
        }
        if (request.getSector() != null) {
            company.setSector(request.getSector());
        }
        if (request.getLocation() != null) {
            company.setLocation(request.getLocation());
        }
        if (request.getFoundedYear() != null) {
            company.setFoundedYear(request.getFoundedYear());
        }

        // Update team members
        if (request.getTeamMembers() != null) {
            // Clear existing team members and add new ones
            company.getTeamMembers().clear();
            for (UpdateCompanyProfileRequest.TeamMemberRequest tmRequest : request.getTeamMembers()) {
                CompanyTeamMember teamMember = new CompanyTeamMember(
                        company,
                        tmRequest.getName(),
                        tmRequest.getRole(),
                        tmRequest.getBio()
                );
                teamMember.setImageUrl(tmRequest.getImageUrl());
                company.getTeamMembers().add(teamMember);
            }
        }

        // Update social media
        if (request.getSocialMedia() != null) {
            CompanySocialMedia socialMedia = company.getSocialMedia();
            if (socialMedia == null) {
                socialMedia = CompanySocialMedia.builder()
                        .company(company)
                        .build();
                company.setSocialMedia(socialMedia);
            }
            UpdateCompanyProfileRequest.SocialMediaRequest smRequest = request.getSocialMedia();
            if (smRequest.getWebsite() != null) {
                socialMedia.setWebsite(smRequest.getWebsite());
            }
            if (smRequest.getLinkedin() != null) {
                socialMedia.setLinkedin(smRequest.getLinkedin());
            }
            if (smRequest.getFacebook() != null) {
                socialMedia.setFacebook(smRequest.getFacebook());
            }
            if (smRequest.getTwitter() != null) {
                socialMedia.setTwitter(smRequest.getTwitter());
            }
            if (smRequest.getInstagram() != null) {
                socialMedia.setInstagram(smRequest.getInstagram());
            }
        }

        companyRepository.save(company);
        return convertToProfileDto(company);
    }

    @Transactional(readOnly = true)
    public CompanyDashboardDto getCompanyDashboard() {
        Company company = getCurrentCompany();
        CompanyProfileDto profile = convertToProfileDto(company);

        // Get investments for this company
        List<Investment> investments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getCompany().getId().equals(company.getId()))
                .collect(Collectors.toList());

        // Calculate stats
        long totalInvestments = investments.size();
        long activeInvestments = investments.stream()
                .filter(inv -> inv.getDeadline() != null && inv.getDeadline().isAfter(java.time.LocalDate.now()))
                .count();

        BigDecimal totalRaised = investments.stream()
                .map(Investment::getRaisedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarget = investments.stream()
                .map(Investment::getTargetAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get total investors across all investments
        long totalInvestors = investments.stream()
                .flatMap(inv -> investorInvestmentRepository.findByInvestment(inv).stream())
                .map(InvestorInvestment::getInvestor)
                .distinct()
                .count();

        BigDecimal averageInvestmentAmount = BigDecimal.ZERO;
        if (totalInvestors > 0) {
            averageInvestmentAmount = totalRaised.divide(BigDecimal.valueOf(totalInvestors), 2, java.math.RoundingMode.HALF_UP);
        }

        CompanyDashboardDto.DashboardStatsDto stats = CompanyDashboardDto.DashboardStatsDto.builder()
                .totalInvestments(totalInvestments)
                .activeInvestments(activeInvestments)
                .totalRaised(totalRaised)
                .totalTarget(totalTarget)
                .totalInvestors(totalInvestors)
                .averageInvestmentAmount(averageInvestmentAmount)
                .build();

        // Get recent investments (last 5)
        List<InvestmentDto> recentInvestments = investments.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return CompanyDashboardDto.builder()
                .companyProfile(profile)
                .stats(stats)
                .recentInvestments(recentInvestments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvestmentDto> getCompanyInvestments() {
        Company company = getCurrentCompany();
        List<Investment> investments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getCompany().getId().equals(company.getId()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        return investments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private CompanyProfileDto convertToProfileDto(Company company) {
        CompanyProfileDto.CompanyProfileDtoBuilder builder = CompanyProfileDto.builder()
                .id(company.getId())
                .name(company.getName())
                .description(company.getDescription())
                .logo(company.getLogo())
                .sector(company.getSector())
                .location(company.getLocation())
                .foundedYear(company.getFoundedYear())
                .documents(company.getDocuments() != null ? new ArrayList<>(company.getDocuments()) : new ArrayList<>());

        // Add user info
        if (company.getUser() != null) {
            builder.email(company.getUser().getEmail())
                    .contactName(company.getUser().getName());
        }

        // Convert team members
        if (company.getTeamMembers() != null) {
            builder.teamMembers(company.getTeamMembers().stream()
                    .map(tm -> CompanyProfileDto.TeamMemberDto.builder()
                            .id(tm.getId())
                            .name(tm.getName())
                            .role(tm.getRole())
                            .bio(tm.getBio())
                            .imageUrl(tm.getImageUrl())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Convert financials
        if (company.getFinancials() != null) {
            builder.financials(company.getFinancials().stream()
                    .map(f -> CompanyProfileDto.CompanyFinancialDto.builder()
                            .id(f.getId())
                            .year(f.getYear())
                            .revenue(f.getRevenue())
                            .expenses(f.getExpenses())
                            .profit(f.getProfit())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Convert social media
        if (company.getSocialMedia() != null) {
            CompanySocialMedia sm = company.getSocialMedia();
            builder.socialMedia(CompanyProfileDto.SocialMediaDto.builder()
                    .website(sm.getWebsite())
                    .linkedin(sm.getLinkedin())
                    .facebook(sm.getFacebook())
                    .twitter(sm.getTwitter())
                    .instagram(sm.getInstagram())
                    .build());
        }

        return builder.build();
    }

    @Transactional
    public void deleteInvestment(Long investmentId) {
        Company company = getCurrentCompany();
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found"));

        // Verify ownership
        if (!investment.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("You do not have permission to delete this investment");
        }

        investmentRepository.delete(investment);
    }

    @Transactional(readOnly = true)
    public Page<CompanyProfileDto> getAllCompanies(int page, int size, CompanySector sector, Location location) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Company> companies;
        
        if (sector != null && location != null) {
            companies = companyRepository.findBySectorAndLocation(sector, location, pageable);
        } else if (sector != null) {
            companies = companyRepository.findBySector(sector, pageable);
        } else if (location != null) {
            companies = companyRepository.findByLocation(location, pageable);
        } else {
            companies = companyRepository.findAll(pageable);
        }
        
        return companies.map(this::convertToProfileDto);
    }

    @Transactional(readOnly = true)
    public Page<InvestorSearchDto> searchInvestors(
            String query,
            BigDecimal minPortfolio,
            RiskLevel riskLevel,
            InvestmentCategory category,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Investor> investors = investorRepository.searchInvestors(
                query, minPortfolio, riskLevel, category, pageable
        );
        
        return investors.map(investor -> {
            InvestorSearchDto.InvestorSearchDtoBuilder builder = InvestorSearchDto.builder()
                    .id(investor.getId())
                    .name(investor.getUser().getName())
                    .email(investor.getUser().getEmail())
                    .portfolioValue(investor.getPortfolio_value());
            
            if (investor.getPreferences() != null) {
                builder.riskLevels(investor.getPreferences().getRiskLevels().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()));
                builder.categories(investor.getPreferences().getCategories().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()));
                builder.locations(investor.getPreferences().getLocations().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()));
            }
            
            return builder.build();
        });
    }

    @Transactional
    public void submitVerificationDocuments(CompanyVerificationRequest request) {
        Company company = getCurrentCompany();
        
        // Check if company is in APPROVED or PENDING_DOCS status (can submit/update step 2)
        // APPROVED: First time submitting verification documents
        // PENDING_DOCS: Resubmitting/updating verification documents
        if (company.getStatus() != CompanyStatus.APPROVED && company.getStatus() != CompanyStatus.PENDING_DOCS) {
            log.warn("Company {} attempted to submit verification documents but status is: {}", 
                    company.getName(), company.getStatus());
            throw new IllegalStateException("Company must be approved (or have pending documents) before submitting verification documents. Current status: " + company.getStatus());
        }
        
        // Check if verification documents already exist
        CompanyVerificationDocuments existing = verificationDocumentsRepository.findByCompany(company)
                .orElse(null);
        
        CompanyVerificationDocuments verificationDocs;
        if (existing != null) {
            // Update existing
            verificationDocs = existing;
        } else {
            // Create new
            verificationDocs = CompanyVerificationDocuments.builder()
                    .company(company)
                    .isApproved(false)
                    .build();
        }
        
        // Update all fields - only update if provided (not null and not empty)
        if (request.getCertificateOfIncorporation() != null && !request.getCertificateOfIncorporation().trim().isEmpty()) {
            verificationDocs.setCertificateOfIncorporation(request.getCertificateOfIncorporation());
        }
        if (request.getArticlesOfAssociation() != null && !request.getArticlesOfAssociation().trim().isEmpty()) {
            verificationDocs.setArticlesOfAssociation(request.getArticlesOfAssociation());
        }
        if (request.getTaxRegistrationCertificate() != null && !request.getTaxRegistrationCertificate().trim().isEmpty()) {
            verificationDocs.setTaxRegistrationCertificate(request.getTaxRegistrationCertificate());
        }
        if (request.getProofOfRegisteredAddress() != null && !request.getProofOfRegisteredAddress().trim().isEmpty()) {
            verificationDocs.setProofOfRegisteredAddress(request.getProofOfRegisteredAddress());
        }
        if (request.getShareholderStructure() != null && !request.getShareholderStructure().trim().isEmpty()) {
            verificationDocs.setShareholderStructure(request.getShareholderStructure());
        }
        
        if (request.getUboIds() != null && !request.getUboIds().isEmpty()) {
            verificationDocs.setUboIds(new ArrayList<>(request.getUboIds()));
        }
        if (request.getDirectorIds() != null && !request.getDirectorIds().isEmpty()) {
            verificationDocs.setDirectorIds(new ArrayList<>(request.getDirectorIds()));
        }
        if (request.getAuthorizedSignatoryIds() != null && !request.getAuthorizedSignatoryIds().isEmpty()) {
            verificationDocs.setAuthorizedSignatoryIds(new ArrayList<>(request.getAuthorizedSignatoryIds()));
        }
        if (request.getBoardResolution() != null && !request.getBoardResolution().trim().isEmpty()) {
            verificationDocs.setBoardResolution(request.getBoardResolution());
        }
        if (request.getPepSanctionsDeclaration() != null && !request.getPepSanctionsDeclaration().trim().isEmpty()) {
            verificationDocs.setPepSanctionsDeclaration(request.getPepSanctionsDeclaration());
        }
        if (request.getBankAccountConfirmation() != null && !request.getBankAccountConfirmation().trim().isEmpty()) {
            verificationDocs.setBankAccountConfirmation(request.getBankAccountConfirmation());
        }
        
        if (request.getFinancialStatements() != null && !request.getFinancialStatements().isEmpty()) {
            verificationDocs.setFinancialStatements(new ArrayList<>(request.getFinancialStatements()));
        }
        if (request.getManagementAccounts() != null && !request.getManagementAccounts().isEmpty()) {
            verificationDocs.setManagementAccounts(new ArrayList<>(request.getManagementAccounts()));
        }
        if (request.getBankStatements() != null && !request.getBankStatements().isEmpty()) {
            verificationDocs.setBankStatements(new ArrayList<>(request.getBankStatements()));
        }
        if (request.getSourceOfFundsDeclaration() != null && !request.getSourceOfFundsDeclaration().trim().isEmpty()) {
            verificationDocs.setSourceOfFundsDeclaration(request.getSourceOfFundsDeclaration());
        }
        
        // Update company status to PENDING_DOCS
        company.setStatus(CompanyStatus.PENDING_DOCS);
        companyRepository.save(company);
        
        verificationDocumentsRepository.save(verificationDocs);
        log.info("Verification documents submitted for company: {}", company.getName());
        
        // Store company ID for notification after transaction commits
        Long companyId = company.getId();
        String companyName = company.getName();
        
        // Notify admins about verification document submission (SSE) - AFTER transaction commits
        // This prevents race condition where async notification might run before transaction commits
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.info(">>> TRIGGERING SSE NOTIFICATION (after commit) for company verification: {} (ID: {}) <<<", 
                                companyName, companyId);
                        // Reload company to ensure we have the latest data after commit
                        Company reloadedCompany = companyRepository.findById(companyId)
                                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
                        adminNotificationSseController.notifyAllAdminsVerification(reloadedCompany);
                        log.info("✓ SSE notification method called successfully for company verification: {}", companyName);
                    } catch (Exception e) {
                        log.error("✗✗✗ FAILED to trigger SSE notification for company verification: {} (ID: {}) - {} ✗✗✗", 
                                companyName, companyId, e.getMessage(), e);
                        // Continue - notification should still be saved in DB
                    }
                }
            }
        );
        
        // Send email notification to admin with verification documents
        try {
            sendAdminVerificationEmail(company, verificationDocs);
            log.info("Email notification initiated for company verification: {}", company.getName());
        } catch (Exception e) {
            log.error("Failed to initiate email notification for company verification: {}", e.getMessage(), e);
            // Continue - verification submission should still succeed
        }
    }
    
    private void sendAdminVerificationEmail(Company company, CompanyVerificationDocuments verificationDocs) {
        try {
            String adminEmail = varsConfig.getAdminEmail();
            if (adminEmail == null || adminEmail.isEmpty()) {
                log.error("Admin email is not configured in VarsConfig");
                return;
            }
            
            log.info("Preparing to send verification email to admin: {}", adminEmail);
            String adminDashboardUrl = varsConfig.getFrontendUrl() + "/admin-dashboard";
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("companyName", company.getName());
            templateData.put("representativeName", company.getUser().getName());
            templateData.put("email", company.getUser().getEmail());
            templateData.put("adminDashboardUrl", adminDashboardUrl);
            
            log.info("Loading email template: CompanyVerificationAdminNotification");
            String htmlContent = mailService.loadAndFormatEmailTemplate(templateData, "CompanyVerificationAdminNotification");
            log.info("Email template loaded successfully, sending email to: {}", adminEmail);
            
            mailService.sendHtmlMail(adminEmail, "Company Verification Documents Submitted: " + company.getName(), htmlContent);
            
            log.info("Admin verification notification email queued for sending to: {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send admin verification notification email: {}", e.getMessage(), e);
            e.printStackTrace();
            // Don't throw - verification submission should still succeed even if email fails
        }
    }
    
    @Transactional(readOnly = true)
    public CompanyVerificationRequest getVerificationDocuments() {
        Company company = getCurrentCompany();
        CompanyVerificationDocuments docs = verificationDocumentsRepository.findByCompany(company)
                .orElse(null);
        
        if (docs == null) {
            return null;
        }
        
        return CompanyVerificationRequest.builder()
                .certificateOfIncorporation(docs.getCertificateOfIncorporation())
                .articlesOfAssociation(docs.getArticlesOfAssociation())
                .taxRegistrationCertificate(docs.getTaxRegistrationCertificate())
                .proofOfRegisteredAddress(docs.getProofOfRegisteredAddress())
                .shareholderStructure(docs.getShareholderStructure())
                .uboIds(docs.getUboIds() != null ? new ArrayList<>(docs.getUboIds()) : null)
                .directorIds(docs.getDirectorIds() != null ? new ArrayList<>(docs.getDirectorIds()) : null)
                .authorizedSignatoryIds(docs.getAuthorizedSignatoryIds() != null ? new ArrayList<>(docs.getAuthorizedSignatoryIds()) : null)
                .boardResolution(docs.getBoardResolution())
                .pepSanctionsDeclaration(docs.getPepSanctionsDeclaration())
                .bankAccountConfirmation(docs.getBankAccountConfirmation())
                .financialStatements(docs.getFinancialStatements() != null ? new ArrayList<>(docs.getFinancialStatements()) : null)
                .managementAccounts(docs.getManagementAccounts() != null ? new ArrayList<>(docs.getManagementAccounts()) : null)
                .bankStatements(docs.getBankStatements() != null ? new ArrayList<>(docs.getBankStatements()) : null)
                .sourceOfFundsDeclaration(docs.getSourceOfFundsDeclaration())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Validate that new password and confirmation match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation password do not match");
        }

        // Get current company
        Company company = getCurrentCompany();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), company.getUser().getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update password
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        company.getUser().setPassword(encodedNewPassword);
        companyRepository.save(company);
        log.info("Password changed successfully for company: {}", company.getName());
    }

    @Transactional
    public String uploadProfileImage(MultipartFile file) {
        Company company = getCurrentCompany();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed.");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new BadRequestException("File size cannot exceed 5MB.");
        }

        try {
            String projectRoot = System.getProperty("user.dir");
            Path uploadsBasePath = Paths.get(projectRoot, "uploads", "companies", String.valueOf(company.getId()), "profile");

            Files.createDirectories(uploadsBasePath);
            log.info("Profile image upload directory: {}", uploadsBasePath.toAbsolutePath());

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            String baseFileName = originalFileName;
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < originalFileName.length() - 1) {
                fileExtension = originalFileName.substring(lastDotIndex);
                baseFileName = originalFileName.substring(0, lastDotIndex);
            }

            String sanitizedBaseName = baseFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String uniqueFileName = System.currentTimeMillis() + "_" + sanitizedBaseName + fileExtension;

            Path targetFilePath = uploadsBasePath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            log.info("Profile image uploaded successfully to: {}", targetFilePath.toAbsolutePath());

            String relativePath = "uploads/companies/" + company.getId() + "/profile/" + uniqueFileName;
            company.setLogo(relativePath);
            companyRepository.save(company);

            return relativePath;
        } catch (IOException e) {
            log.error("Failed to save profile image file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save profile image file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during profile image upload: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred during profile image upload: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<InvestmentRequestDto> getInvestmentRequests(String status) {
        Company company = getCurrentCompany();
        
        com.lebvest.model.enums.InvestmentRequestStatus requestStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                requestStatus = com.lebvest.model.enums.InvestmentRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, return all
                requestStatus = null;
            }
        }
        
        List<com.lebvest.model.entities.investment.InvestmentRequest> requests;
        if (requestStatus != null) {
            requests = investmentRequestRepository.findByInvestment_Company_IdAndStatusOrderByCreatedAtDesc(
                    company.getId(), requestStatus);
        } else {
            // Get all requests for this company
            requests = investmentRequestRepository.findAll().stream()
                    .filter(req -> req.getInvestment().getCompany().getId().equals(company.getId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        }
        
        return requests.stream()
                .map(this::convertToInvestmentRequestDto)
                .collect(Collectors.toList());
    }

    private InvestmentRequestDto convertToInvestmentRequestDto(com.lebvest.model.entities.investment.InvestmentRequest request) {
        return InvestmentRequestDto.builder()
                .id(request.getId())
                .investorId(request.getInvestor().getId())
                .investorName(request.getInvestor().getUser().getName())
                .investorEmail(request.getInvestor().getUser().getEmail())
                .investorProfileImageUrl(request.getInvestor().getImageUrl())
                .investmentId(request.getInvestment().getId())
                .investmentTitle(request.getInvestment().getTitle())
                .amount(request.getAmount())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .message(request.getMessage())
                .stripePaymentIntentId(request.getStripePaymentIntentId())
                .paidAt(request.getPaidAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .acceptedAt(request.getAcceptedAt())
                .rejectedAt(request.getRejectedAt())
                .build();
    }
}

