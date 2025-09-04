package com.lebvest.model.entities.company;

import com.lebvest.model.entities.investment.FundingHistory;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.CompanySector;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
@RequiredArgsConstructor
@SuperBuilder
@Data
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(length = 512)
    private String logo;

    @Lob
    private String description;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private int foundedYear;

    @Column(nullable = false)
    private CompanySector sector;

    private String location;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<CompanyTeamMember> teamMembers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "company_documents",
            joinColumns = @JoinColumn(name = "company_id", referencedColumnName = "id")
    )
    @Column(name = "document")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<String> documents = new ArrayList<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<CompanyFinancial> financials = new ArrayList<>();

    @OneToOne(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CompanySocialMedia socialMedia;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<CompanyInvestor> investors = new ArrayList<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<FundingHistory> fundingHistory = new ArrayList<>();
}
