package com.lebvest.model.dto.investor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestorProfileRequest {
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 512, message = "Image URL must not exceed 512 characters")
    private String imageUrl;

    private Boolean profilePublic;
}


