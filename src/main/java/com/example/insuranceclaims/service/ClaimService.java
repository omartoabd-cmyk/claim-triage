package com.example.insuranceclaims.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.insuranceclaims.dto.AnalysisResponse;
import com.example.insuranceclaims.dto.ClaimRequest;
import com.example.insuranceclaims.dto.ClaimResponse;
import com.example.insuranceclaims.exception.ClaimNotFoundException;
import com.example.insuranceclaims.exception.DuplicateClaimException;
import com.example.insuranceclaims.model.Claim;
import com.example.insuranceclaims.model.ClaimAnalysis;
import com.example.insuranceclaims.model.ClaimStatus;
import com.example.insuranceclaims.repository.ClaimRepository;

import java.time.Instant;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final AIService aiService;
    private final AnalysisRateLimiter rateLimiter;
    private final TransactionTemplate tx;

    public ClaimService(ClaimRepository claimRepository,
                        AIService aiService,
                        AnalysisRateLimiter rateLimiter,
                        PlatformTransactionManager txManager) {
        this.claimRepository = claimRepository;
        this.aiService = aiService;
        this.rateLimiter = rateLimiter;
        this.tx = new TransactionTemplate(txManager);
    }

    @Transactional
    public ClaimResponse createClaim(ClaimRequest request) {
        String claimNumber = request.claimNumber().trim();
        if (claimRepository.existsByClaimNumber(claimNumber)) {
            throw new DuplicateClaimException(claimNumber);
        }

        Claim claim = new Claim();
        claim.setClaimNumber(claimNumber);
        claim.setPolicyNumber(request.policyNumber().trim());
        claim.setClaimType(request.claimType());
        claim.setIncidentDate(request.incidentDate());
        claim.setClaimAmount(request.claimAmount());
        claim.setDescription(request.description().trim());

        try {
            return ClaimResponse.from(claimRepository.saveAndFlush(claim));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent insert of the same claim number.
            throw new DuplicateClaimException(claimNumber);
        }
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(ClaimResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long id) {
        return ClaimResponse.from(findClaim(id));
    }

    @Transactional
    public ClaimResponse updateStatus(Long id, ClaimStatus status) {
        Claim claim = findClaim(id);
        claim.setStatus(status);
        return ClaimResponse.from(claim);
    }

    /**
     * Returns the stored analysis if there is one (free), otherwise calls the AI
     * and persists the result. {@code refresh} forces a new AI call.
     *
     * <p>Deliberately not {@code @Transactional}: we must not hold a database
     * transaction open during the slow network call.
     */
    public AnalysisResponse analyzeClaim(Long id, boolean refresh, String clientId) {
        Claim claim = claimRepository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));

        if (!refresh && claim.getAnalysis() != null) {
            return tx.execute(status -> AnalysisResponse.from(findClaim(id).getAnalysis()));
        }

        rateLimiter.acquire(clientId);
        AIService.AnalysisResult result = aiService.analyzeClaim(claim);

        return tx.execute(status -> {
            Claim managed = findClaim(id);
            ClaimAnalysis analysis = managed.getAnalysis();
            if (analysis == null) {
                analysis = new ClaimAnalysis();
                analysis.setClaim(managed);
                managed.setAnalysis(analysis);
            }
            analysis.setSummary(result.summary());
            analysis.setSuggestedCategory(result.suggestedCategory());
            analysis.setRiskLevel(result.riskLevel());
            analysis.setMissingInformation(result.missingInformation());
            analysis.setRecommendedNextSteps(result.recommendedNextSteps());
            analysis.setAnalyzedAt(Instant.now());
            analysis.setModel(aiService.getModel());
            claimRepository.saveAndFlush(managed);
            return AnalysisResponse.from(analysis);
        });
    }

    private Claim findClaim(Long id) {
        return claimRepository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }
}
