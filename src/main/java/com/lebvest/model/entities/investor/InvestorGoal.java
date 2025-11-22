package com.lebvest.model.entities.investor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name="investor_goals",
        indexes=@Index(name="idx_goals_investor", columnList="investor_id")
)
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class InvestorGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NonNull
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    @NonNull
    @Column(name = "title", nullable = false)
    private String name;

    @NonNull
    @Column(name = "target_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(nullable = true)
    private LocalDate deadline;

    @PrePersist
    public void prePersist() {
        if (currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
    }
}
