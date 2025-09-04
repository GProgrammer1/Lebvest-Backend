package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Maps to the investment_highlights table:
 *
 * CREATE TABLE investment_highlights (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   investment_id    CHAR(36)       NOT NULL REFERENCES investments(id),
 *   highlight        TEXT           NOT NULL
 * );
 * CREATE INDEX idx_ih_investment ON investment_highlights(investment_id);
 */
@Entity
@Table(
        name = "investment_highlights",
        indexes = @Index(name = "idx_ih_investment", columnList = "investment_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many highlights belong to one investment.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /**
     * A single highlight or bullet point for the investment.
     */
    @Lob
    @Column(nullable = false)
    private String highlight;
}
