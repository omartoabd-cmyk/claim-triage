package com.example.insuranceclaims.exception;

public class ClaimNotFoundException extends RuntimeException {
    public ClaimNotFoundException(Long id) {
        super("Claim " + id + " not found");
    }
}
