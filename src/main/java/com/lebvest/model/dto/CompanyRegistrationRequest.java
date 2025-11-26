package com.lebvest.model.dto;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email(message = "Email must be of a valid format")
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String companyName;

    @NotBlank
    private String description;

    @NotNull
    private CompanySector sector;

    // Documents are optional - can be null or empty
    private MultipartFile[] documents;

    @NotBlank
    private String location;

    private int foundedYear;
//    private final Role role = Role.COMPANY;
@Override
public String toString() {
    return "CompanyRegistrationRequest{" +
            "companyName='" + companyName + '\'' +
            ", description='" + description + '\'' +
            ", sector='" + sector + '\'' +
            ", foundedYear=" + foundedYear +
            ", location='" + location + '\'' +
            ", email='" + email + '\'' +
            ", name='" + name + '\'' +
            ", password='[PROTECTED]'" +  // mask sensitive data!
            ", documents=" + Arrays.toString(documents) +
            '}';
}

}
