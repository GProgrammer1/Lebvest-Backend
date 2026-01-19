package com.lebvest.service;

import com.lebvest.model.dto.CreateInvestmentRequestRequest;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.InvestmentRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class InvestmentRequestServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvestmentRequestService investmentRequestService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    @Test
    void testCreateInvestmentRequestThrowsExceptionWhenNotVerified() {
        Long investmentId = 1L;
        CreateInvestmentRequestRequest request = new CreateInvestmentRequestRequest();
        request.setAmount(BigDecimal.valueOf(1000.0));

        User user = User.builder().email("investor@test.com").build();
        Investor investor = Investor.builder()
                .kycVerified(false)
                .build();

        Investment investment = Investment.builder()
                .id(investmentId)
                .build();

        // Mock Security Context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("investor@test.com");

        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(user));
        when(investorRepository.findByUser(user)).thenReturn(Optional.of(investor));
        when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(investment));

        assertThrows(IllegalStateException.class, () -> {
            investmentRequestService.createInvestmentRequest(investmentId, request);
        });
    }
}
