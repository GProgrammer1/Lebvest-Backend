package com.lebvest.model.dto;

import com.lebvest.model.enums.CompanySector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileDto {
    private Long id;
    private String name;
    private String description;
    private String logo;
    private CompanySector sector;
    private String location;
    private Integer foundedYear;
    private String email; // From user
    private String contactName; // From user
    private List<TeamMemberDto> teamMembers;
    private List<String> documents;
    private List<CompanyFinancialDto> financials;
    private SocialMediaDto socialMedia;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberDto {
        private Long id;
        private String name;
        private String role;
        private String bio;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyFinancialDto {
        private Long id;
        private Integer year;
        private java.math.BigDecimal revenue;
        private java.math.BigDecimal expenses;
        private java.math.BigDecimal profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialMediaDto {
        private String website;
        private String linkedin;
        private String facebook;
        private String twitter;
        private String instagram;
    }
}

