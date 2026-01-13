package com.lebvest.service;

import com.lebvest.model.entities.admin.AdminNotification;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.AdminNotificationType;
import com.lebvest.model.enums.VerificationStatus;
import com.lebvest.repository.AdminNotificationRepository;
import com.lebvest.repository.InvestorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private AdminNotificationRepository adminNotificationRepository;

    @Mock
    private IMailService mailService;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPendingInvestorVerifications() {
        Investor investor = new Investor();
        investor.setKycStatus(VerificationStatus.PENDING);
        Page<Investor> page = new PageImpl<>(Collections.singletonList(investor));

        when(investorRepository.findByKycStatus(eq(VerificationStatus.PENDING), any())).thenReturn(page);

        Page<Investor> resultPage = (Page<Investor>) adminService.getPendingInvestorVerifications(0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        verify(investorRepository).findByKycStatus(eq(VerificationStatus.PENDING), any());
    }

    @Test
    void testApproveInvestorVerification() {
        Long investorId = 1L;
        User user = User.builder().email("investor@example.com").name("Test Investor").build();
        Investor investor = Investor.builder()
                .id(investorId)
                .user(user)
                .kycStatus(VerificationStatus.PENDING)
                .kycVerified(false)
                .build();

        AdminNotification notification = new AdminNotification();
        notification.setInvestor(investor);
        notification.setRead(false);
        notification.setType(AdminNotificationType.VERIFICATION_REQUEST);

        when(investorRepository.findById(investorId)).thenReturn(Optional.of(investor));
        when(adminNotificationRepository.findByInvestorAndReadFalse(investor))
                .thenReturn(Collections.singletonList(notification));
        when(mailService.loadAndFormatEmailTemplate(any(), anyString()))
                .thenReturn("<html>Test Content</html>");

        adminService.approveInvestorVerification(investorId);

        assertTrue(investor.getKycVerified());
        assertEquals(VerificationStatus.APPROVED, investor.getKycStatus());
        assertTrue(notification.isRead());
        verify(investorRepository).save(investor);
        verify(adminNotificationRepository).save(notification);
        verify(mailService).sendHtmlMail(eq("investor@example.com"), anyString(), anyString());
    }

    @Test
    void testRejectInvestorVerification() {
        Long investorId = 1L;
        User user = User.builder().email("investor@example.com").name("Test Investor").build();
        Investor investor = Investor.builder()
                .id(investorId)
                .user(user)
                .kycStatus(VerificationStatus.PENDING)
                .kycVerified(false)
                .build();

        AdminNotification notification = new AdminNotification();
        notification.setInvestor(investor);
        notification.setRead(false);
        notification.setType(AdminNotificationType.VERIFICATION_REQUEST);

        when(investorRepository.findById(investorId)).thenReturn(Optional.of(investor));
        when(adminNotificationRepository.findByInvestorAndReadFalse(investor))
                .thenReturn(Collections.singletonList(notification));
        when(mailService.loadAndFormatEmailTemplate(any(), anyString()))
                .thenReturn("<html>Test Content</html>");

        adminService.rejectInvestorVerification(investorId, "Documents are blurred");

        assertFalse(investor.getKycVerified());
        assertEquals(VerificationStatus.REJECTED, investor.getKycStatus());
        assertTrue(notification.isRead());
        verify(investorRepository).save(investor);
        verify(adminNotificationRepository).save(notification);
        verify(mailService).sendHtmlMail(eq("investor@example.com"), anyString(), anyString());
    }
}
