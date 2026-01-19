package com.lebvest.service;

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

class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvestmentService investmentService;

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
    void testMakeInvestmentThrowsExceptionWhenNotVerified() {
        Long investmentId = 1L;
        BigDecimal amount = BigDecimal.valueOf(1000.0);

        User user = User.builder().email("investor@test.com").build();
        Investor investor = Investor.builder()
                .kycVerified(false)
                .build();

        Investment investment = Investment.builder()
                .id(investmentId)
                .minInvestment(BigDecimal.valueOf(100.0))
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
            investmentService.makeInvestment(investmentId, amount);
        });
    }
}
