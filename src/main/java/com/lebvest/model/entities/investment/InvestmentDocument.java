package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Maps to the investment_documents table:
 *
 * CREATE TABLE investment_documents (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   investment_id    CHAR(36)       NOT NULL REFERENCES investments(id),
 *   title            VARCHAR(255)   NOT NULL,
 *   type             VARCHAR(100)   NOT NULL,
 *   url              VARCHAR(512)   NOT NULL
 * );
 * CREATE INDEX idx_idoc_investment ON investment_documents(investment_id);
 */
@Entity
@Table(
        name = "investment_documents",
        indexes = @Index(name = "idx_idoc_investment", columnList = "investment_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many documents belong to one investment.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /**
     * Human-readable title of the document.
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Type/category of the document (e.g. \"Whitepaper\", \"Pitch Deck\").
     */
    @Column(nullable = false, length = 100)
    private String type;

    /**
     * URL where the document is stored (e.g. S3 link).
     */
    @Column(nullable = false, length = 512)
    private String url;
}
