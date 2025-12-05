package com.lebvest.model.entities.admin;

import com.lebvest.model.entities.company.CompanySignupRequest;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.AdminNotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(name = "admin_notifications")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", referencedColumnName = "id")
    private User admin;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private AdminNotificationType type;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;


    private boolean isRead;
    private Boolean isAccepted;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", referencedColumnName = "id", nullable = true)
    private CompanySignupRequest request;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_id", referencedColumnName = "id", nullable = true)
    private com.lebvest.model.entities.investment.Investment investment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "id", nullable = true)
    private com.lebvest.model.entities.company.Company company;
    
    @PrePersist
    protected void onCreate() {
        isRead = false;
    }


}
