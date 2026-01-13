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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private InvestorRegistrationService investorRegistrationService;

    @BeforeEach
    void setUp() {
        investorRepository = mock(InvestorRepository.class);
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        fileStorageService = mock(IFileStorageService.class);
        adminNotificationRepository = mock(AdminNotificationRepository.class);

        investorRegistrationService = new InvestorRegistrationService(
                investorRepository,
                userRepository,
                jwtService,
                passwordEncoder,
                fileStorageService,
                adminNotificationRepository);
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

        // Mock admin search
        User admin = User.builder().id(1L).name("Admin").email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(userRepository.findAll()).thenReturn(List.of(admin));

        // Act
        String token = investorRegistrationService.registerInvestor(request);

        // Assert
        assertEquals("mockToken", token);
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<Investor> investorCaptor = ArgumentCaptor.forClass(Investor.class);
        verify(investorRepository).save(investorCaptor.capture());

        Investor savedInvestor = investorCaptor.getValue();
        assertEquals(com.lebvest.model.enums.VerificationStatus.PENDING, savedInvestor.getKycStatus());
        assertFalse(savedInvestor.getKycVerified());
        assertEquals("path/to/doc", savedInvestor.getIdentityDocUrl());
        assertEquals("path/to/doc", savedInvestor.getAddressDocUrl());
        assertEquals("path/to/doc", savedInvestor.getSelfieDocUrl());
        assertEquals("path/to/doc", savedInvestor.getSourceOfFundsDocUrl());

        verify(adminNotificationRepository).save(any(AdminNotification.class));
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

        // Act
        String token = investorRegistrationService.registerInvestor(request);

        // Assert
        assertEquals("mockToken", token);
        assertTrue(existingUser.getRoles().contains(Role.INVESTOR));
        verify(userRepository).save(existingUser);
        verify(investorRepository).save(any(Investor.class));
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
