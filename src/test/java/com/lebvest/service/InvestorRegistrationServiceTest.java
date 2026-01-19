package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.InvestorRegistrationRequest;
import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvestorRegistrationServiceTest {

    private InvestorRepository investorRepository;
    private UserRepository userRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private IFileStorageService fileStorageService;
    private AdminNotificationRepository adminNotificationRepository;
    private IMailService mailService;
    private WebSocketNotificationService webSocketNotificationService;

    private InvestorRegistrationService investorRegistrationService;

    @BeforeEach
    void setUp() {
        investorRepository = mock(InvestorRepository.class);
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        fileStorageService = mock(IFileStorageService.class);
        adminNotificationRepository = mock(AdminNotificationRepository.class);
        mailService = mock(IMailService.class);
        webSocketNotificationService = mock(WebSocketNotificationService.class);

        investorRegistrationService = new InvestorRegistrationService(
                investorRepository,
                userRepository,
                jwtService,
                passwordEncoder,
                fileStorageService,
                adminNotificationRepository,
                mailService,
                webSocketNotificationService);
    }

    @Test
    void registerInvestor_Success_NewUser() throws IOException {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("mockToken");
        when(fileStorageService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("path/to/doc");

        // Mock admin search (called multiple times: once in createAdminNotifications, once in afterCommit)
        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        // Mock investor repository to return saved investor with ID
        // Use Answer to capture the investor and set its ID
        when(investorRepository.save(any(Investor.class))).thenAnswer(invocation -> {
            Investor investor = invocation.getArgument(0);
            // Set ID on the investor object
            try {
                java.lang.reflect.Field idField = Investor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(investor, 100L);
            } catch (Exception e) {
                // If reflection fails, create a new investor with ID
                investor = Investor.builder()
                        .id(100L)
                        .user(investor.getUser())
                        .bio(investor.getBio())
                        .preferences(investor.getPreferences())
                        .kycStatus(investor.getKycStatus())
                        .kycVerified(investor.getKycVerified())
                        .identityDocUrl(investor.getIdentityDocUrl())
                        .addressDocUrl(investor.getAddressDocUrl())
                        .selfieDocUrl(investor.getSelfieDocUrl())
                        .sourceOfFundsDocUrl(investor.getSourceOfFundsDocUrl())
                        .build();
            }
            return investor;
        });
        when(investorRepository.findById(100L)).thenAnswer(invocation -> {
            // Return the investor that was saved
            return Optional.of(Investor.builder()
                    .id(100L)
                    .user(User.builder().id(1L).email(request.getEmail()).name(request.getName()).build())
                    .kycStatus(com.lebvest.model.enums.VerificationStatus.PENDING)
                    .kycVerified(false)
                    .identityDocUrl("path/to/doc")
                    .addressDocUrl("path/to/doc")
                    .selfieDocUrl("path/to/doc")
                    .sourceOfFundsDocUrl("path/to/doc")
                    .build());
        });

        // Mock TransactionSynchronizationManager to simulate active transaction
        try (MockedStatic<TransactionSynchronizationManager> mockedManager = 
                mockStatic(TransactionSynchronizationManager.class)) {
            mockedManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        // Immediately execute the afterCommit callback for unit test
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // Act
            String token = investorRegistrationService.registerInvestor(request);

            // Assert
            assertEquals("mockToken", token);
            verify(userRepository).save(any(User.class));

            ArgumentCaptor<Investor> investorCaptor = ArgumentCaptor.forClass(Investor.class);
            verify(investorRepository).save(investorCaptor.capture());

            Investor capturedInvestor = investorCaptor.getValue();
            assertEquals(com.lebvest.model.enums.VerificationStatus.PENDING, capturedInvestor.getKycStatus());
            assertFalse(capturedInvestor.getKycVerified());
            assertEquals("path/to/doc", capturedInvestor.getIdentityDocUrl());
            assertEquals("path/to/doc", capturedInvestor.getAddressDocUrl());
            assertEquals("path/to/doc", capturedInvestor.getSelfieDocUrl());
            assertEquals("path/to/doc", capturedInvestor.getSourceOfFundsDocUrl());

            // Verify preferences were set correctly
            assertNotNull(capturedInvestor.getPreferences());
            assertEquals(Set.of(InvestmentCategory.TECHNOLOGY), capturedInvestor.getPreferences().getCategories());
            assertEquals(Set.of(Location.BEIRUT), capturedInvestor.getPreferences().getLocations());
            assertEquals(Set.of(RiskLevel.MEDIUM), capturedInvestor.getPreferences().getRiskLevels());

            // Verify admin notification was created
            verify(adminNotificationRepository).save(any(AdminNotification.class));
            verify(webSocketNotificationService).notifyAdmin(eq(admin.getId()), any(AdminNotification.class));
            
            // Verify email service was called (after commit callback executed)
            // The afterCommit callback calls sendAdminNotificationEmails which calls userRepository.findAll() again
            verify(userRepository, atLeast(1)).findAll();
            verify(mailService).sendSimpleMail(eq(admin.getEmail()), contains("New Investor Registration"), anyString());
        }
    }

    @Test
    void registerInvestor_ThrowsException_MissingIdentityDoc() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setIdentityDoc(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MissingAddressDoc() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setAddressDoc(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MissingSelfieDoc() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setSelfieDoc(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MissingSourceOfFundsDoc() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setSourceOfFundsDoc(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_Success_ExistingUser() throws IOException {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        User existingUser = User.builder().id(2L).email(request.getEmail()).name("Existing").roles(Set.of()).build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));
        when(investorRepository.existsByUser(existingUser)).thenReturn(false);
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("mockToken");
        when(fileStorageService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("path/to/doc");

        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        // Use Answer to set ID on saved investor
        when(investorRepository.save(any(Investor.class))).thenAnswer(invocation -> {
            Investor investor = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = Investor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(investor, 100L);
            } catch (Exception e) {
                investor = Investor.builder().id(100L).user(investor.getUser()).build();
            }
            return investor;
        });
        when(investorRepository.findById(100L)).thenReturn(Optional.of(
                Investor.builder().id(100L).user(existingUser).build()));

        // Mock TransactionSynchronizationManager
        try (MockedStatic<TransactionSynchronizationManager> mockedManager = 
                mockStatic(TransactionSynchronizationManager.class)) {
            mockedManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // Act
            String token = investorRegistrationService.registerInvestor(request);

            // Assert
            assertEquals("mockToken", token);
            assertTrue(existingUser.getRoles().contains(Role.INVESTOR));
            verify(userRepository).save(existingUser);
            verify(investorRepository).save(any(Investor.class));
        }
    }

    @Test
    void registerInvestor_ThrowsConflict_ProfileExists() {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        User user = User.builder().id(2L).email(request.getEmail()).build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(investorRepository.existsByUser(user)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MissingCategories() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setInvestmentCategories(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MissingLocations() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setLocations(Set.of());
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_NoRiskLevel() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setRiskLevels(null);
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_ThrowsException_MultipleRiskLevels() {
        InvestorRegistrationRequest request = createValidRequest();
        request.setRiskLevels(Set.of(RiskLevel.LOW, RiskLevel.HIGH));
        assertThrows(IllegalArgumentException.class, () -> investorRegistrationService.registerInvestor(request));
    }

    @Test
    void registerInvestor_Success_ValidatesPreferences() throws IOException {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("mockToken");
        when(fileStorageService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("path/to/doc");

        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        // Use Answer to set ID on saved investor
        when(investorRepository.save(any(Investor.class))).thenAnswer(invocation -> {
            Investor investor = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = Investor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(investor, 100L);
            } catch (Exception e) {
                investor = Investor.builder().id(100L).user(investor.getUser()).build();
            }
            return investor;
        });
        when(investorRepository.findById(100L)).thenReturn(Optional.of(
                Investor.builder()
                        .id(100L)
                        .user(User.builder().id(1L).email(request.getEmail()).name(request.getName()).build())
                        .build()));

        // Mock TransactionSynchronizationManager
        try (MockedStatic<TransactionSynchronizationManager> mockedManager = 
                mockStatic(TransactionSynchronizationManager.class)) {
            mockedManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // Act
            String token = investorRegistrationService.registerInvestor(request);

            // Assert
            assertEquals("mockToken", token);
            
            ArgumentCaptor<Investor> investorCaptor = ArgumentCaptor.forClass(Investor.class);
            verify(investorRepository).save(investorCaptor.capture());
            
            Investor capturedInvestor = investorCaptor.getValue();
            assertNotNull(capturedInvestor.getPreferences());
            assertEquals(1, capturedInvestor.getPreferences().getCategories().size());
            assertEquals(1, capturedInvestor.getPreferences().getLocations().size());
            assertEquals(1, capturedInvestor.getPreferences().getRiskLevels().size());
        }
    }

    @Test
    void registerInvestor_Success_MultipleAdmins_AllNotified() throws IOException {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("mockToken");
        when(fileStorageService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("path/to/doc");

        // Mock multiple admins (called multiple times: once in createAdminNotifications, once in afterCommit)
        User admin1 = User.builder().id(1L).name("Admin 1").email("admin1@test.com").roles(Set.of(Role.ADMIN)).build();
        User admin2 = User.builder().id(2L).name("Admin 2").email("admin2@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin1, admin2));

        // Use Answer to set ID on saved investor
        when(investorRepository.save(any(Investor.class))).thenAnswer(invocation -> {
            Investor investor = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = Investor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(investor, 100L);
            } catch (Exception e) {
                investor = Investor.builder().id(100L).user(investor.getUser()).build();
            }
            return investor;
        });
        when(investorRepository.findById(100L)).thenReturn(Optional.of(
                Investor.builder()
                        .id(100L)
                        .user(User.builder().id(1L).email(request.getEmail()).name(request.getName()).build())
                        .build()));

        // Mock TransactionSynchronizationManager
        try (MockedStatic<TransactionSynchronizationManager> mockedManager = 
                mockStatic(TransactionSynchronizationManager.class)) {
            mockedManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // Act
            investorRegistrationService.registerInvestor(request);

            // Assert - Both admins should receive notifications
            verify(adminNotificationRepository, times(2)).save(any(AdminNotification.class));
            verify(webSocketNotificationService).notifyAdmin(eq(admin1.getId()), any(AdminNotification.class));
            verify(webSocketNotificationService).notifyAdmin(eq(admin2.getId()), any(AdminNotification.class));
            
            // The afterCommit callback calls sendAdminNotificationEmails which calls userRepository.findAll() again
            verify(userRepository, atLeast(1)).findAll();
            verify(mailService, times(2)).sendSimpleMail(anyString(), contains("New Investor Registration"), anyString());
        }
    }

    @Test
    void registerInvestor_Success_FileUploadsCalledCorrectly() throws IOException {
        // Arrange
        InvestorRegistrationRequest request = createValidRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("mockToken");
        when(fileStorageService.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("path/to/doc");

        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        // Use Answer to set ID on saved investor
        when(investorRepository.save(any(Investor.class))).thenAnswer(invocation -> {
            Investor investor = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = Investor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(investor, 100L);
            } catch (Exception e) {
                investor = Investor.builder().id(100L).user(investor.getUser()).build();
            }
            return investor;
        });
        when(investorRepository.findById(100L)).thenReturn(Optional.of(
                Investor.builder()
                        .id(100L)
                        .user(User.builder().id(1L).email(request.getEmail()).name(request.getName()).build())
                        .build()));

        // Mock TransactionSynchronizationManager
        try (MockedStatic<TransactionSynchronizationManager> mockedManager = 
                mockStatic(TransactionSynchronizationManager.class)) {
            mockedManager.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            mockedManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            // Act
            investorRegistrationService.registerInvestor(request);

            // Assert - Verify all 4 file uploads were called with correct prefixes
            verify(fileStorageService, times(4)).uploadFile(anyString(), anyString(), any(), anyLong(), anyString());
            verify(fileStorageService).uploadFile(eq("investor/identity"), anyString(), any(), anyLong(), anyString());
            verify(fileStorageService).uploadFile(eq("investor/address"), anyString(), any(), anyLong(), anyString());
            verify(fileStorageService).uploadFile(eq("investor/selfie"), anyString(), any(), anyLong(), anyString());
            verify(fileStorageService).uploadFile(eq("investor/source-of-funds"), anyString(), any(), anyLong(), anyString());
        }
    }

    private InvestorRegistrationRequest createValidRequest() {
        return InvestorRegistrationRequest.builder()
                .name("John Investor")
                .email("john@investor.com")
                .password("securePass")
                .bio("I love startups")
                .investmentCategories(Set.of(InvestmentCategory.TECHNOLOGY))
                .locations(Set.of(Location.BEIRUT))
                .riskLevels(Set.of(RiskLevel.MEDIUM))
                .identityDoc(new MockMultipartFile("identity", "id.pdf", "application/pdf", "content".getBytes()))
                .addressDoc(new MockMultipartFile("address", "address.pdf", "application/pdf", "content".getBytes()))
                .selfieDoc(new MockMultipartFile("selfie", "selfie.jpg", "image/jpeg", "content".getBytes()))
                .sourceOfFundsDoc(new MockMultipartFile("sof", "sof.pdf", "application/pdf", "content".getBytes()))
                .build();
    }
}
