package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(
        name = "investment_updates",
        indexes = @Index(name = "idx_iup_investment", columnList = "investment_id")
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class InvestmentUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many updates belong to one investment. */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /** Date of this update. */
    @NonNull
    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;

    /** Title of the update. */
    @NonNull
    @Column(nullable = false, length = 255)
    private String title;

    /** Content/body of the update. */
    @NonNull
    @Lob
    @Column(nullable = false)
    private String content;
}
