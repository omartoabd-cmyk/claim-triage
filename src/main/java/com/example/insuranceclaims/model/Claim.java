package com.example.insuranceclaims.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String claimNumber;

    @Column(nullable = false, length = 50)
    private String policyNumber;

    @Column(nullable = false, length = 30)
    private String claimType;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal claimAmount;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private ClaimAnalysis analysis;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ClaimStatus.SUBMITTED;
        }
    }

    public Long getId() { return id; }

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public String getClaimType() { return claimType; }
    public void setClaimType(String claimType) { this.claimType = claimType; }

    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }

    public BigDecimal getClaimAmount() { return claimAmount; }
    public void setClaimAmount(BigDecimal claimAmount) { this.claimAmount = claimAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public ClaimAnalysis getAnalysis() { return analysis; }
    public void setAnalysis(ClaimAnalysis analysis) { this.analysis = analysis; }
}
