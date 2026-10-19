package com.example.insuranceclaims.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.insuranceclaims.model.Claim;
import com.example.insuranceclaims.model.ClaimStatus;

public record ClaimResponse(
        Long id,
        String claimNumber,
        String policyNumber,
        String claimType,
        LocalDate incidentDate,
        BigDecimal claimAmount,
        String description,
        ClaimStatus status,
        Instant createdAt,
        AnalysisResponse analysis) {

    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(
                c.getId(),
                c.getClaimNumber(),
                c.getPolicyNumber(),
                c.getClaimType(),
                c.getIncidentDate(),
                c.getClaimAmount(),
                c.getDescription(),
                c.getStatus(),
                c.getCreatedAt(),
                AnalysisResponse.from(c.getAnalysis()));
    }
}
