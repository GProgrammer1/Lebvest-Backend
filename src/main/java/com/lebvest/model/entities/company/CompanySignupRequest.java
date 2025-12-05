package com.lebvest.model.entities.company;

import com.lebvest.model.dto.CompanyDocumentEntry;
import com.lebvest.model.enums.CompanySector;
import com.lebvest.model.enums.SignupRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name = "company_signup_requests"

        )
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

public class CompanySignupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID requestId = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String logo;

    @ElementCollection
    @CollectionTable(
            name = "requesting_company_documents",
            joinColumns = @JoinColumn(name = "signup_request", referencedColumnName = "id")
    )
    private List<String> documents = new ArrayList<>();



    @Column(nullable = false)
    @NotBlank
    private String companyName;

    private CompanySector sector;

    // Custom sector text when sector is OTHER
    @Column(length = 255, nullable = true)
    private String customSector;

    @CurrentTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private SignupRequestStatus requestStatus = SignupRequestStatus.PENDING;

    @Column(nullable = false)
    private int foundedYear;

    @Column(nullable = false, length = 255)
    private String governorate;

    @Column(nullable = false, length = 255)
    private String city;

    @Column(nullable = true, length = 512)
    private String location; // Optional - can be built from governorate + city later

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String website;

    @PrePersist
    public void ensureDefaults() {
        if (this.requestStatus == null) {
            this.requestStatus = SignupRequestStatus.PENDING;
        }
        if (this.requestId == null) {
            this.requestId = UUID.randomUUID();
        }
    }


}
