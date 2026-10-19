package com.example.insuranceclaims.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/** The persisted result of an AI triage run for one claim. */
@Entity
@Table(name = "claim_analyses")
public class ClaimAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(nullable = false, length = 200)
    private String suggestedCategory;

    /** Always one of "Low", "Medium", "High". */
    @Column(nullable = false, length = 10)
    private String riskLevel;

    @ElementCollection
    @CollectionTable(name = "claim_analysis_missing_info", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "position")
    @Column(name = "item", length = 1000)
    @BatchSize(size = 50)
    private List<String> missingInformation = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "claim_analysis_next_steps", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "position")
    @Column(name = "item", length = 1000)
    @BatchSize(size = 50)
    private List<String> recommendedNextSteps = new ArrayList<>();

    @Column(nullable = false)
    private Instant analyzedAt;

    @Column(length = 100)
    private String model;

    public ClaimAnalysis() {
    }

    public Long getId() { return id; }

    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(String suggestedCategory) { this.suggestedCategory = suggestedCategory; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getMissingInformation() { return missingInformation; }
    public void setMissingInformation(List<String> missingInformation) {
        this.missingInformation = new ArrayList<>(missingInformation);
    }

    public List<String> getRecommendedNextSteps() { return recommendedNextSteps; }
    public void setRecommendedNextSteps(List<String> recommendedNextSteps) {
        this.recommendedNextSteps = new ArrayList<>(recommendedNextSteps);
    }

    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
