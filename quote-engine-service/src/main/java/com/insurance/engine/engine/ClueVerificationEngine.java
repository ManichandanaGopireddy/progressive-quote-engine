package com.insurance.engine.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClueVerificationEngine {

    @Value("${clue.rule.clean.prefixes}")
    private String cleanPrefixes;

    @Value("${clue.rule.minor.prefixes}")
    private String minorPrefixes;

    @Value("${clue.rule.minor.extra-accidents}")
    private int minorExtraAccidents;

    @Value("${clue.rule.moderate.prefixes}")
    private String moderatePrefixes;

    @Value("${clue.rule.moderate.extra-accidents}")
    private int moderateExtraAccidents;

    @Value("${clue.rule.moderate.extra-violations}")
    private boolean moderateExtraViolations;

    @Value("${clue.rule.high-risk.prefixes}")
    private String highRiskPrefixes;

    @Value("${clue.rule.high-risk.extra-accidents}")
    private int highRiskExtraAccidents;

    @Value("${clue.rule.high-risk.sr22-override}")
    private boolean highRiskSr22Override;

    @Value("${clue.rule.suspended.prefixes}")
    private String suspendedPrefixes;

    @Value("${clue.rule.suspended.license-override}")
    private String suspendedLicenseOverride;

    public ClueResult verify(Map<String, Object> driver) {
        String licenseNumber = (String) driver.get("licenseNumber");
        String firstName = (String) driver.get("firstName");
        String lastName = (String) driver.get("lastName");
        Object accidentsObj = driver.get("numberOfAccidents");
        int reportedAccidents = accidentsObj instanceof Number
                ? ((Number) accidentsObj).intValue() : 0;
        boolean reportedViolations =
                Boolean.TRUE.equals(driver.get("violations"));
        boolean reportedSr22 =
                Boolean.TRUE.equals(driver.get("sr22Required"));
        String reportedLicenseStatus =
                (String) driver.get("licenseStatus");

        if (licenseNumber == null || licenseNumber.isBlank()) {
            log.warn("No license number for driver {} {} — defaulting to CLEAN",
                    firstName, lastName);
            return ClueResult.builder()
                    .riskLevel("CLEAN")
                    .verifiedAccidents(reportedAccidents)
                    .verifiedViolations(reportedViolations)
                    .sr22Override(reportedSr22)
                    .licenseStatusOverride(reportedLicenseStatus)
                    .discrepancyFound(false)
                    .discrepancyDetails("No license number provided")
                    .build();
        }

        String prefix = String.valueOf(
                licenseNumber.toUpperCase().charAt(0));
        String riskLevel = determineRiskLevel(prefix);

        int verifiedAccidents = reportedAccidents;
        boolean verifiedViolations = reportedViolations;
        boolean sr22Override = reportedSr22;
        String licenseStatusOverride = reportedLicenseStatus;
        boolean discrepancyFound = false;
        StringBuilder discrepancyDetails = new StringBuilder();

        switch (riskLevel) {
            case "MINOR" -> {
                verifiedAccidents = reportedAccidents + minorExtraAccidents;
                if (verifiedAccidents != reportedAccidents) {
                    discrepancyFound = true;
                    discrepancyDetails.append(
                            "CLUE records show ")
                            .append(minorExtraAccidents)
                            .append(" additional accident(s). ");
                }
            }
            case "MODERATE" -> {
                verifiedAccidents =
                        reportedAccidents + moderateExtraAccidents;
                verifiedViolations = moderateExtraViolations;
                discrepancyFound = true;
                discrepancyDetails.append(
                        "CLUE records show ")
                        .append(moderateExtraAccidents)
                        .append(" additional accident(s) and violations. ");
            }
            case "HIGH_RISK" -> {
                verifiedAccidents =
                        reportedAccidents + highRiskExtraAccidents;
                sr22Override = highRiskSr22Override;
                discrepancyFound = true;
                discrepancyDetails.append(
                        "CLUE records show ")
                        .append(highRiskExtraAccidents)
                        .append(" additional accident(s). SR22 required. ");
            }
            case "SUSPENDED" -> {
                licenseStatusOverride = suspendedLicenseOverride;
                discrepancyFound = true;
                discrepancyDetails.append(
                        "CLUE records show license status: SUSPENDED. ");
            }
        }

        log.info("CLUE verification for {} {}: prefix={} riskLevel={}",
                firstName, lastName, prefix, riskLevel);

        return ClueResult.builder()
                .riskLevel(riskLevel)
                .verifiedAccidents(verifiedAccidents)
                .verifiedViolations(verifiedViolations)
                .sr22Override(sr22Override)
                .licenseStatusOverride(licenseStatusOverride)
                .discrepancyFound(discrepancyFound)
                .discrepancyDetails(discrepancyDetails.toString().trim())
                .build();
    }

    private String determineRiskLevel(String prefix) {
        if (containsPrefix(cleanPrefixes, prefix))    return "CLEAN";
        if (containsPrefix(minorPrefixes, prefix))    return "MINOR";
        if (containsPrefix(moderatePrefixes, prefix)) return "MODERATE";
        if (containsPrefix(highRiskPrefixes, prefix)) return "HIGH_RISK";
        if (containsPrefix(suspendedPrefixes, prefix)) return "SUSPENDED";
        return "CLEAN";
    }

    private boolean containsPrefix(String prefixList, String prefix) {
        return Arrays.asList(prefixList.split(",")).contains(prefix);
    }

    @lombok.Builder
    @lombok.Data
    public static class ClueResult {
        private String riskLevel;
        private int verifiedAccidents;
        private boolean verifiedViolations;
        private boolean sr22Override;
        private String licenseStatusOverride;
        private boolean discrepancyFound;
        private String discrepancyDetails;
    }
}