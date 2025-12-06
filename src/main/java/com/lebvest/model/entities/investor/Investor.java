package com.lebvest.model.entities.investor;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.entities.investment.InvestorInvestment;
import jakarta.persistence.*;
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
@EqualsAndHashCode(exclude = {"preferences", "watchlist", "investments", "notifications", "goals"})
@ToString(exclude = {"preferences", "watchlist", "investments", "notifications", "goals"})
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

    @Column(name = "profile_public", nullable = false)
    private Boolean profilePublic = false;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "investor_watchlist",
            joinColumns = @JoinColumn(name = "investor_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "investment_id", referencedColumnName = "id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Investment> watchlist = new HashSet<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestorInvestment> investments = new ArrayList<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestorNotification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "investor", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestorGoal> goals = new ArrayList<>();

    @OneToOne(mappedBy = "investor",
            cascade = CascadeType.ALL, orphanRemoval = true)
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
    }
}
