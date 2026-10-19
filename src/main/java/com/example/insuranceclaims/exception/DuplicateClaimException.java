package com.example.insuranceclaims.exception;

public class DuplicateClaimException extends RuntimeException {
    public DuplicateClaimException(String claimNumber) {
        super("A claim with number " + claimNumber + " already exists");
    }
}
