package com.lebvest.model.dto.investor;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestorPreferenceRequest {
    @NotEmpty(message = "Categories cannot be empty")
    private Set<InvestmentCategory> categories;

    @NotEmpty(message = "Risk levels cannot be empty")
    private Set<RiskLevel> riskLevels;

    @NotEmpty(message = "Locations cannot be empty")
    private Set<Location> locations;
}


