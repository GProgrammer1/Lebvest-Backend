package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.CompanyRepository;
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
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private CompanyRegistrationService companyRegistrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        companyRepository = mock(CompanyRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);

        companyRegistrationService = new CompanyRegistrationService(
                userRepository,
                companyRepository,
                passwordEncoder,
                jwtService
        );
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

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mock(User.class)));

        assertThrows(ConflictException.class, () ->
                companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldThrowConflict_ifCompanyNameAlreadyExists() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setCompanyName("TestCorp");
        request.setName("Test User");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.of(mock(Company.class)));

        assertThrows(ConflictException.class, () ->
                companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldCreateUserAndCompany_returnJwtToken() {
        CompanyRegistrationRequest request = getCompanyRegistrationRequest();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        Company savedCompany = mock(Company.class);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);
        
        when(jwtService.generateToken(any(), eq("access"), eq(1L))).thenReturn("jwt-token");

        String result = companyRegistrationService.registerCompany(request, bindingResult);

        assertNotNull(result);
        assertEquals("jwt-token", result);
        verify(userRepository).save(any(User.class));
        verify(companyRepository).save(any(Company.class));
        verify(jwtService).generateToken(any(), eq("access"), eq(1L));
    }

    private static CompanyRegistrationRequest getCompanyRegistrationRequest() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setCompanyName("TestCorp");
        request.setDescription("Test description");
        request.setLocation("BEIRUT");
        request.setFoundedYear(2023);
        request.setName("John Doe");
        request.setPassword("password123");
        request.setSector(com.lebvest.model.enums.CompanySector.TECHNOLOGY);
        return request;
    }
}
