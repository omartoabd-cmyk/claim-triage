package com.example.insuranceclaims.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.insuranceclaims.dto.AnalysisResponse;
import com.example.insuranceclaims.dto.ClaimRequest;
import com.example.insuranceclaims.dto.ClaimResponse;
import com.example.insuranceclaims.dto.StatusUpdateRequest;
import com.example.insuranceclaims.service.ClaimService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping
    public List<ClaimResponse> getAllClaims() {
        return claimService.getAllClaims();
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaim(@PathVariable Long id) {
        return claimService.getClaimById(id);
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request) {
        ClaimResponse saved = claimService.createClaim(request);
        return ResponseEntity.created(URI.create("/api/claims/" + saved.id())).body(saved);
    }

    @PatchMapping("/{id}/status")
    public ClaimResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return claimService.updateStatus(id, request.status());
    }

    /**
     * Returns the saved analysis, or runs one. Pass {@code ?refresh=true} to
     * force a new (rate-limited, paid) AI call.
     */
    @PostMapping("/{id}/analyze")
    public AnalysisResponse analyzeClaim(@PathVariable Long id,
                                         @RequestParam(defaultValue = "false") boolean refresh,
                                         HttpServletRequest http) {
        // getRemoteAddr on purpose: X-Forwarded-For is client-controlled and would
        // let anyone dodge the rate limit. Configure forwarded-header handling at
        // the proxy if you deploy behind one.
        return claimService.analyzeClaim(id, refresh, http.getRemoteAddr());
    }
}
