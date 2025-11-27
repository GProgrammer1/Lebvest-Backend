package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorSearchDto {
    private Long id;
    private String name;
    private String email;
    private BigDecimal portfolioValue;
    private Set<String> riskLevels;
    private Set<String> categories;
    private Set<String> locations;
}

