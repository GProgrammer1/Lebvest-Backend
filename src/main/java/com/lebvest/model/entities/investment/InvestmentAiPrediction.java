package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maps to the investment_ai_predictions table:
 *
 * CREATE TABLE investment_ai_predictions (
 *   investment_id     CHAR(36)       PRIMARY KEY REFERENCES investments(id),
 *   profit_prediction DECIMAL(5,2)   NOT NULL,
 *   risk_assessment   VARCHAR(255)   NOT NULL,
 *   confidence_score  DECIMAL(5,2)   NOT NULL
 * );
 */
@Entity
@Table(name = "investment_ai_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentAiPrediction {

    /**
     * Shared‐primary‐key mapping: this field is both PK and FK to Investment.id
     */
    @Id
    @Column(name = "investment_id")
    private Long investmentId;

    /**
     * The Investment entity this prediction belongs to.
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /**
     * Predicted profit percentage (e.g. 12.34).
     */
    @Column(name = "profit_prediction", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal profitPrediction;

    /**
     * Narrative risk assessment from the AI model.
     */
    @Column(name = "risk_assessment", nullable = false, length = 255)
    private String riskAssessment;

    /**
     * Confidence score (0–100) of the prediction.
     */
    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal confidenceScore;
}
