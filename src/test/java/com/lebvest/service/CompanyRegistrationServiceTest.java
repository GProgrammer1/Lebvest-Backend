package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.controller.AdminNotificationSseController;
import com.lebvest.exception.ConflictException;
import com.lebvest.model.dto.CompanyRegistrationRequest;
import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.CompanySignupRequestRepository;
import com.lebvest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyRegistrationServiceTest {

    private VarsConfig varsConfig;
    private CompanySignupRequestRepository companySignupRequestRepository;
    private AdminNotificationSseController adminNotificationSseController;
    private CompanyRegistrationService companyRegistrationService;
    private RabbitTemplate rabbitTemplate;
    @BeforeEach
    void setUp() {
        varsConfig = mock(VarsConfig.class);
        UserRepository userRepository = mock(UserRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        companySignupRequestRepository = mock(CompanySignupRequestRepository.class);
        adminNotificationSseController = mock(AdminNotificationSseController.class);
        rabbitTemplate = mock(RabbitTemplate.class);

        companyRegistrationService = new CompanyRegistrationService(
                varsConfig,
                userRepository,
                companyRepository,
                companySignupRequestRepository,
                adminNotificationSseController,
                rabbitTemplate
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
    void registerCompany_shouldThrowConflict_ifCompanyEmailAlreadyExists() {
        CompanyRegistrationRequest request = new CompanyRegistrationRequest();
        request.setEmail("test@example.com");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(companySignupRequestRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mock(CompanySignupRequest.class)));

        assertThrows(ConflictException.class, () ->
                companyRegistrationService.registerCompany(request, bindingResult));
    }

    @Test
    void registerCompany_shouldSaveRequest_notifyAdmins_enqueueEvents() {
        CompanyRegistrationRequest request = getCompanyRegistrationRequest();//creates a mock registration request

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false); //checks if binding is correct
        when(companySignupRequestRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        UUID fakeRequestId = UUID.randomUUID();
        when(varsConfig.getPendingPrefix(any())).thenReturn("pending/" + fakeRequestId); //testing the pending prefix method
        when(varsConfig.getSignupCompanyUploadQueueName()).thenReturn("uploadQueue");
        when(varsConfig.getSignupCompanyEmailQueueName()).thenReturn("emailQueue");

        doNothing().when(adminNotificationSseController).notifyAllAdmins(any());

        String result = companyRegistrationService.registerCompany(request, bindingResult);

        assertEquals("Request submitted successfully", result);
        verify(companySignupRequestRepository, times(2)).save(any());
        verify(adminNotificationSseController).notifyAllAdmins(any());
        verify(rabbitTemplate).convertAndSend(eq("uploadQueue"), any(Object.class));
        verify(rabbitTemplate).convertAndSend(eq("emailQueue"), any(Object.class));
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
