package com.example.insuranceclaims.model;

public enum ClaimStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    DENIED;

    /** Claims that still need a human to look at them. */
    public boolean isOpen() {
        return this == SUBMITTED || this == IN_REVIEW;
    }
}
