package com.lebvest.model.entities;

import com.lebvest.model.entities.investor.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forgot_pass_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ForgotPassToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String token;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
