package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Maps to the investment_team_members table:
 *
 * CREATE TABLE investment_team_members (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   investment_id    CHAR(36)       NOT NULL REFERENCES investments(id),
 *   name             VARCHAR(255)   NOT NULL,
 *   role             VARCHAR(255)   NOT NULL,
 *   bio              TEXT           NOT NULL,
 *   image_url        VARCHAR(512)
 * );
 * CREATE INDEX idx_itm_investment ON investment_team_members(investment_id);
 */
@Entity
@Table(
        name = "investment_team_members",
        indexes = @Index(name = "idx_itm_investment", columnList = "investment_id")
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class InvestmentTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many team members belong to one investment. */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /** Full name of the team member. */
    @NonNull
    @Column(nullable = false, length = 255)
    private String name;

    /** Role/title within the investment project. */
    @NonNull
    @Column(nullable = false, length = 255)
    private String role;

    /** Short biography or description. */
    @NonNull
    @Lob
    @Column(nullable = false)
    private String bio;

    /** Optional URL to a profile image. */
    @Column(name = "image_url", length = 512)
    private String imageUrl;
}
