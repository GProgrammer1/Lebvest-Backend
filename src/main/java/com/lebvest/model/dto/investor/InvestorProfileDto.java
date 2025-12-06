package com.lebvest.model.dto.investor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class InvestorProfileDto {
    private Long id;
    private String name;
    private String email;
    private String bio;
    private String imageUrl;
    private BigDecimal portfolioValue;
    private BigDecimal totalInvested;
    private BigDecimal totalReturns;
    private Boolean profilePublic;
}


