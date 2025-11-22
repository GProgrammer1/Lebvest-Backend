package com.lebvest.model.entities.investor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "investors")
@RequiredArgsConstructor
@Data
@SuperBuilder
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal portfolio_value = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total_invested = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total_returns = BigDecimal.ZERO;

    private String bio;

    private String imageUrl;

    // OK — ManyToMany Set (this is fine)
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "investor_watchlist",
            joinColumns = @JoinColumn(name = "investor_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "investment_id", referencedColumnName = "id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<Investment> watchlist = new HashSet<>();

    // FIX: Use Set instead of List to avoid bag fetch exception
    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<InvestorInvestment> investments = new HashSet<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<InvestorNotification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Set<InvestorGoal> goals = new HashSet<>();

    @OneToOne(mappedBy = "investor",
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private InvestorPreference preferences;

    @PrePersist
    public void prePersist() {
        if (portfolio_value == null) portfolio_value = BigDecimal.ZERO;
        if (total_invested == null) total_invested = BigDecimal.ZERO;
        if (total_returns == null) total_returns = BigDecimal.ZERO;
    }
}
