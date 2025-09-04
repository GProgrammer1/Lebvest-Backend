package com.lebvest.model.entities.investor;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
public class InvestorGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NonNull
    @ManyToOne(fetch=LAZY, optional=false)
    @JoinColumn(name="investor_id", nullable=false)
    private Investor investor;

    @NonNull private String title;
    @NonNull @Column(name="target_amount", precision=15, scale=2) private BigDecimal targetAmount;
    @NonNull @Column(name="current_amount", precision=15, scale=2) private BigDecimal currentAmount;
    @NonNull private LocalDate deadline;
}
