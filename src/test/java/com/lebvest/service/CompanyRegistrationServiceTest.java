package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.controller.AdminNotificationSseController;
import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompanyRegistrationServiceTest {

    private UserRepository userRepository;
    private CompanyRepository companyRepository;
    private CompanySignupRequestRepository companySignupRequestRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private LocalFileStorageService localFileStorageService;
    private MailService mailService;
    private VarsConfig varsConfig;
    private AdminNotificationSseController adminNotificationSseController;
    private CompanyRegistrationService companyRegistrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        companyRepository = mock(CompanyRepository.class);
        companySignupRequestRepository = mock(CompanySignupRequestRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        localFileStorageService = mock(LocalFileStorageService.class);
        mailService = mock(MailService.class);
        varsConfig = mock(VarsConfig.class);
        adminNotificationSseController = mock(AdminNotificationSseController.class);

        companyRegistrationService = new CompanyRegistrationService(
                userRepository,
                companyRepository,
                companySignupRequestRepository,
                passwordEncoder,
                jwtService,
                localFileStorageService,
                mailService,
                varsConfig,
                adminNotificationSseController);
    }

    @Test
    void registerCompany_shouldThrowException_ifBindingResultHasErrors() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        BindingResult bindingResult = mock(BindingResult.class);

        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("field", "Some error")));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> companyRegistrationService.registerCompany(request, bindingResult));

        assertTrue(exception.getMessage().contains("Validation failed"));
    }

    @Test
    void registerCompany_shouldThrowConflict_ifUserEmailAlreadyExists() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setCompanyName("TestCorp");
        request.setName("Test User");
        request.setGovernorate("Beirut");
        request.setCity("Beirut");
        request.setPhoneNumber("+961 1 234 567");
        request.setWebsite("https://www.test.com");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mock(User.class)));

        assertThrows(ConflictException.class, () -> companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldThrowConflict_ifCompanyNameAlreadyExists() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setCompanyName("TestCorp");
        request.setName("Test User");
        request.setGovernorate("Beirut");
        request.setCity("Beirut");
        request.setPhoneNumber("+961 1 234 567");
        request.setWebsite("https://www.test.com");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName()))
                .thenReturn(Optional.of(mock(com.lebvest.model.entities.company.Company.class)));

        assertThrows(ConflictException.class, () -> companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldThrowConflict_ifSignupRequestAlreadyExists() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setCompanyName("TestCorp");
        request.setName("Test User");
        request.setGovernorate("Beirut");
        request.setCity("Beirut");
        request.setPhoneNumber("+961 1 234 567");
        request.setWebsite("https://www.test.com");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.empty());
        when(companySignupRequestRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(mock(CompanySignupRequest.class)));

        assertThrows(ConflictException.class, () -> companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldCreateSignupRequest_sendEmailAndNotification() {
        CompanyRegistrationRequest request = getCompanyRegistrationRequest();

        // Add mock documents to trigger savePendingFiles
        org.springframework.web.multipart.MultipartFile mockFile = mock(
                org.springframework.web.multipart.MultipartFile.class);
        request.setDocuments(new org.springframework.web.multipart.MultipartFile[] { mockFile });

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.empty());
        when(companySignupRequestRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(localFileStorageService.savePendingFiles(any(), any())).thenReturn(List.of("uploads/pending/file1.pdf"));
        when(varsConfig.getAdminEmail()).thenReturn("admin@example.com");
        when(varsConfig.getFrontendUrl()).thenReturn("http://localhost:3000");
        
        // Mock loadAndFormatEmailTemplate to return HTML content
        when(mailService.loadAndFormatEmailTemplate(any(), anyString())).thenReturn("<html>Test Email Content</html>");

        CompanySignupRequest savedRequest = mock(CompanySignupRequest.class);
        when(companySignupRequestRepository.save(any(CompanySignupRequest.class))).thenReturn(savedRequest);

        // Should not throw exception
        assertDoesNotThrow(() -> companyRegistrationService.registerCompany(request, bindingResult));

        verify(companySignupRequestRepository, times(2)).saveAndFlush(any(CompanySignupRequest.class));
        verify(localFileStorageService).savePendingFiles(any(), any());
        verify(mailService).loadAndFormatEmailTemplate(any(), eq("CompanyRegistrationEmail"));
        verify(mailService).loadAndFormatEmailTemplate(any(), eq("CompanySignupAdminNotification"));
        verify(mailService, times(2)).sendHtmlMail(anyString(), anyString(), anyString());
        verify(adminNotificationSseController).notifyAllAdmins(any(CompanySignupRequest.class));
    }

    private static CompanyRegistrationRequest getCompanyRegistrationRequest() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setCompanyName("TestCorp");
        request.setGovernorate("Beirut");
        request.setCity("Beirut");
        request.setPhoneNumber("+961 1 234 567");
        request.setWebsite("https://www.testcorp.com");
        request.setFoundedYear(2023);
        request.setName("John Doe");
        request.setPassword("password123");
        request.setSector(com.lebvest.model.enums.CompanySector.TECHNOLOGY);
        return request;
    }
}
