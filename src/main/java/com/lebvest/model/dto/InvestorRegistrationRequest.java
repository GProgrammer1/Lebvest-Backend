package com.lebvest.model.dto;

import com.lebvest.model.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

public class InvestorRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String bio;

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotEmpty
    @NotNull
    private Set<RiskLevel> riskLevels;

    @NotEmpty
    @NotNull
    private Set<InvestmentCategory> investmentCategories;

    @NotEmpty
    @NotNull
    private Set<Location> locations;

    @NotNull
    private MultipartFile identityDoc;

    @NotNull
    private MultipartFile addressDoc;

    @NotNull
    private MultipartFile selfieDoc;

    @NotNull
    private MultipartFile sourceOfFundsDoc;

    private final Role role = Role.INVESTOR;
}
