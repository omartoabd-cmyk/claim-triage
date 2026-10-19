package com.example.insuranceclaims.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.insuranceclaims.exception.AiServiceException;
import com.example.insuranceclaims.exception.ClaimNotFoundException;
import com.example.insuranceclaims.exception.DuplicateClaimException;
import com.example.insuranceclaims.exception.GlobalExceptionHandler;
import com.example.insuranceclaims.exception.RateLimitExceededException;
import com.example.insuranceclaims.service.ClaimService;

/** Web-layer test: real controller + validation + exception handler, mocked service. */
class ClaimControllerTest {

    private ClaimService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ClaimService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ClaimController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String claimJson(String claimNumber, String amount, String date) {
        return """
                {"claimNumber":"%s","policyNumber":"POL-1","claimType":"Auto",
                 "incidentDate":"%s","claimAmount":%s,"description":"Bumper damage"}"""
                .formatted(claimNumber, date, amount);
    }

    @Test
    void invalidClaimReturns400WithFieldErrorsAndNeverReachesService() throws Exception {
        String body = """
                {"claimNumber":"","policyNumber":"POL-1","claimType":"Boat",
                 "incidentDate":"2999-01-01","claimAmount":-5,"description":""}""";

        mvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.claimNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.claimType").exists())
                .andExpect(jsonPath("$.fieldErrors.incidentDate").exists())
                .andExpect(jsonPath("$.fieldErrors.claimAmount").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists());

        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateClaimNumberReturns409() throws Exception {
        when(service.createClaim(any())).thenThrow(new DuplicateClaimException("CLM-1"));

        mvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(claimJson("CLM-1", "10.50", "2026-01-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void missingClaimReturns404() throws Exception {
        when(service.getClaimById(99L)).thenThrow(new ClaimNotFoundException(99L));

        mvc.perform(get("/api/claims/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Claim 99 not found"));
    }

    @Test
    void analyzeRateLimitedReturns429WithRetryAfter() throws Exception {
        when(service.analyzeClaim(eq(1L), anyBoolean(), anyString()))
                .thenThrow(new RateLimitExceededException("Too many analysis requests.", 42));

        mvc.perform(post("/api/claims/1/analyze"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"));
    }

    @Test
    void analyzeAiFailureReturns503WithoutLeakingDetails() throws Exception {
        when(service.analyzeClaim(eq(1L), anyBoolean(), anyString()))
                .thenThrow(new AiServiceException("The AI service is temporarily unavailable.",
                        new RuntimeException("secret provider detail")));

        mvc.perform(post("/api/claims/1/analyze"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The AI service is temporarily unavailable."));
    }
}
