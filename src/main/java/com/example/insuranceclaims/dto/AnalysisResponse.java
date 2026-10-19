package com.example.insuranceclaims.dto;

import java.time.Instant;
import java.util.List;

import com.example.insuranceclaims.model.ClaimAnalysis;

public record AnalysisResponse(
        String summary,
        String suggestedCategory,
        String riskLevel,
        List<String> missingInformation,
        List<String> recommendedNextSteps,
        Instant analyzedAt,
        String model) {

    public static AnalysisResponse from(ClaimAnalysis a) {
        if (a == null) {
            return null;
        }
        return new AnalysisResponse(
                a.getSummary(),
                a.getSuggestedCategory(),
                a.getRiskLevel(),
                List.copyOf(a.getMissingInformation()),
                List.copyOf(a.getRecommendedNextSteps()),
                a.getAnalyzedAt(),
                a.getModel());
    }
}
