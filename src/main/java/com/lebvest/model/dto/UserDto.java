package com.lebvest.model.dto;

import com.lebvest.model.enums.CompanyStatus;
import com.lebvest.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<Role> roles;
    private String status; // "active", "inactive", "locked", "pending"
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin; // Optional, if you track this
    private boolean enabled;
    private boolean locked;
    
    // Company-specific fields (only populated if user has COMPANY role)
    private CompanyStatus companyStatus; // PENDING, APPROVED, PENDING_DOCS, FULLY_VERIFIED, REJECTED
    private Boolean verificationDocumentsApproved; // true if verification docs are approved
    private Long companyId; // Company ID if user is a company
    
    // Online presence fields
    private Boolean isOnline; // true if user is currently online
    private LocalDateTime lastSeen; // Last time user was seen online
}

