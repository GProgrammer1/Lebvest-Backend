package com.lebvest.model.entities.investor;

import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "investor_preferences")
@Data
@RequiredArgsConstructor
@SuperBuilder
@EqualsAndHashCode(exclude = {"investor"})
@ToString(exclude = {"investor"})
public class InvestorPreference implements Serializable {

    /**
     * PK is investor_id (same as investors.id)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long investorId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "investor_id")
    private Investor investor;

    /**
     * Categories preferences.
     * Backed by table investor_pref_categories(investor_id, category).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "investor_pref_categories",
            joinColumns = @JoinColumn(name = "investor_id")
    )
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<InvestmentCategory> categories = new HashSet<>();

    /**
     * RiskLevel preferences.
     * Backed by table investor_pref_risk_levels(investor_id, risk_level).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "investor_pref_risk_levels",
            joinColumns = @JoinColumn(name = "investor_id")

    )
    @Column(name = "risk_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<RiskLevel> riskLevels = new HashSet<>();

    /**
     * Location preferences.
     * Backed by table investor_pref_locations(investor_id, location).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "investor_pref_locations",
            joinColumns = @JoinColumn(name = "investor_id")
    )
    @Column(name = "location", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Location> locations = new HashSet<>();


}
