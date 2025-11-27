package com.lebvest.service;

import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        request.setCompanyName("TestCorp");

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
        request.setCompanyName("TestCorp");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.of(mock(Company.class)));

        assertThrows(ConflictException.class, () ->
                companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldCreateUserAndCompany_andReturnToken() {
        CompanyRegistrationRequest request = getCompanyRegistrationRequest();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(companyRepository.findByName(request.getCompanyName())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(User.class), eq("access"), anyLong())).thenReturn("test-token");

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(companyRepository.save(any(Company.class))).thenReturn(mock(Company.class));

        String result = companyRegistrationService.registerCompany(request, bindingResult);

        assertTrue(result.equals("test-token"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(companyRepository, times(1)).save(any(Company.class));
        verify(jwtService, times(1)).generateToken(any(User.class), eq("access"), anyLong());
    }

    private static CompanyRegistrationRequest getCompanyRegistrationRequest() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");
        request.setCompanyName("TestCorp");
        request.setDescription("description");
        request.setLocation("location");
        request.setFoundedYear(2023);
        request.setName("John");
        request.setPassword("pass");
        request.setSector(null);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "test content".getBytes());
        request.setDocuments(new MockMultipartFile[]{file});
        return request;
    }
}
