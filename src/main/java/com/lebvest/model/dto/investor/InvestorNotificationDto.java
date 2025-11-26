package com.lebvest.model.dto.investor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class InvestorNotificationDto {
    private Long id;
    private String type;
    private String title;
    private String message;
    private LocalDateTime notifiedAt;
    private boolean read;
    private Long relatedInvestmentId;
}


