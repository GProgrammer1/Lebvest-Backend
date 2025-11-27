package com.lebvest.model.dto;

import com.lebvest.model.enums.CompanySector;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyProfileRequest {
    private String name;
    private String description;
    private String logo;
    private CompanySector sector;
    private String location;
    @Min(value = 1900, message = "Founded year must be after 1900")
    private Integer foundedYear;
    private List<TeamMemberRequest> teamMembers;
    private SocialMediaRequest socialMedia;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberRequest {
        private Long id; // For updates, null for new members
        private String name;
        private String role;
        private String bio;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialMediaRequest {
        private String website;
        private String linkedin;
        private String facebook;
        private String twitter;
        private String instagram;
    }
}

