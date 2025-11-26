package com.lebvest.model.dto.investor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
public class InvestorPreferenceDto {
    private Set<String> categories;
    private Set<String> riskLevels;
    private Set<String> locations;
}


