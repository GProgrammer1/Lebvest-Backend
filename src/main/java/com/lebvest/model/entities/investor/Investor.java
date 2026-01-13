package com.lebvest.model.entities.investor;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "investors")
@RequiredArgsConstructor
@Data
@SuperBuilder
@EqualsAndHashCode(exclude = { "preferences", "watchlist", "investments", "notifications", "goals" })
@ToString(exclude = { "preferences", "watchlist", "investments", "notifications", "goals" })
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal portfolio_value = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total_invested = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total_returns = BigDecimal.ZERO;

    private String bio;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 20)
    private com.lebvest.model.enums.InvestorClassification classification;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "risk_profile_assessment", length = 500)
    private String riskProfileAssessment;

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private Boolean kycVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private com.lebvest.model.enums.VerificationStatus kycStatus = com.lebvest.model.enums.VerificationStatus.PENDING;

    @Column(name = "identity_doc_url")
    private String identityDocUrl;

    @Column(name = "address_doc_url")
    private String addressDocUrl;

    @Column(name = "selfie_doc_url")
    private String selfieDocUrl;

    @Column(name = "source_of_funds_doc_url")
    private String sourceOfFundsDocUrl;

    @Column(name = "kyc_notes", length = 1000)
    private String kycNotes;

    @Column(name = "profile_public", nullable = false)
    @Builder.Default
    private Boolean profilePublic = false;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "investor_watchlist", joinColumns = @JoinColumn(name = "investor_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "investment_id", referencedColumnName = "id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private Set<Investment> watchlist = new HashSet<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private List<InvestorInvestment> investments = new ArrayList<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private List<InvestorNotification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    private List<InvestorGoal> goals = new ArrayList<>();

    @OneToOne(mappedBy = "investor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InvestorPreference preferences;

    @PrePersist
    public void prePersist() {
        if (portfolio_value == null) {
            portfolio_value = BigDecimal.ZERO;
        }
        if (total_invested == null) {
            total_invested = BigDecimal.ZERO;
        }
        if (total_returns == null) {
            total_returns = BigDecimal.ZERO;
        }
        if (profilePublic == null) {
            profilePublic = false;
        }
        if (kycStatus == null) {
            kycStatus = com.lebvest.model.enums.VerificationStatus.PENDING;
        }
    }
}
