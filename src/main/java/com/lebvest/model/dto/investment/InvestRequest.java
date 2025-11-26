package com.lebvest.model.dto.investment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvestRequest {
    private BigDecimal amount;
}
