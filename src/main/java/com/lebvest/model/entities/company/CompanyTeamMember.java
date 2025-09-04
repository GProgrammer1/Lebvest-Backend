package com.lebvest.model.entities.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Maps to the company_team_members table:
 *
 * CREATE TABLE company_team_members (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   company_id       CHAR(36)      NOT NULL REFERENCES companies(id),
 *   name             VARCHAR(255)  NOT NULL,
 *   role             VARCHAR(255)  NOT NULL,
 *   bio              TEXT          NOT NULL,
 *   image_url        VARCHAR(512)
 * );
 * CREATE INDEX idx_ctm_company ON company_team_members(company_id);
 */
@Entity
@Table(
        name = "company_team_members",
        indexes = @Index(name = "idx_ctm_company", columnList = "company_id")
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class CompanyTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many team members belong to one company. */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Full name of the team member. */
    @NonNull
    @Column(nullable = false, length = 255)
    private String name;

    /** Role/title within the company. */
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
