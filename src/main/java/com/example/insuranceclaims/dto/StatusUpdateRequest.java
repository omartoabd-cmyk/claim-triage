package com.example.insuranceclaims.dto;

import com.example.insuranceclaims.model.ClaimStatus;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull ClaimStatus status) {
}
