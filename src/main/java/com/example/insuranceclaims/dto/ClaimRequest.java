package com.example.insuranceclaims.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Inbound claim payload. Deliberately has no id/status/analysis fields, so a
 * client can never overwrite an existing row or pre-set server-owned data.
 */
public record ClaimRequest(
        @NotBlank @Size(max = 50) String claimNumber,
        @NotBlank @Size(max = 50) String policyNumber,
        @NotBlank @Pattern(regexp = "Auto|Home|Health|Property|Liability",
                message = "must be one of Auto, Home, Health, Property, Liability") String claimType,
        @NotNull @PastOrPresent LocalDate incidentDate,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2) BigDecimal claimAmount,
        @NotBlank @Size(max = 2000) String description) {
}
