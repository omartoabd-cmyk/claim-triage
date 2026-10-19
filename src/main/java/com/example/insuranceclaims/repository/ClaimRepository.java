package com.example.insuranceclaims.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.insuranceclaims.model.Claim;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    boolean existsByClaimNumber(String claimNumber);

    /** Newest first, with the analysis loaded in the same query to avoid N+1. */
    @EntityGraph(attributePaths = "analysis")
    List<Claim> findAllByOrderByCreatedAtDescIdDesc();
}
