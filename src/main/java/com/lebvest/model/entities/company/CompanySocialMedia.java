package com.lebvest.model.entities.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name="company_social_media"
)
@RequiredArgsConstructor
@Data
@SuperBuilder
public class CompanySocialMedia {

    @Id
    @MapsId
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    private String website;

    private String linkedin;
    private String facebook;
    private String twitter;
    private String instagram;


}
