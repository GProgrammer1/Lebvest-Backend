package com.lebvest.config;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@org.springframework.lang.NonNull FormatterRegistry registry) {
        registry.addConverter(String.class, InvestmentCategory.class, InvestmentCategory::fromString);
        registry.addConverter(String.class, Location.class, Location::fromString);
        registry.addConverter(String.class, RiskLevel.class, RiskLevel::fromString);
    }
}
