package com.insurance.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.engine.api.model.*;
import com.insurance.engine.client.Api1Client;
import com.insurance.engine.engine.*;
import com.insurance.engine.entity.QuoteEntity;
import com.insurance.engine.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private final Api1Client api1Client;
    private final ClueVerificationEngine clueEngine;
    private final VehicleRatingEngine vehicleRatingEngine;
    private final DriverRiskEngine driverRiskEngine;
    private final PriorInsuranceEngine priorInsuranceEngine;
    private final QuoteRepository quoteRepository;
    private final ObjectMapper objectMapper;

    private static final double BASE_PREMIUM = 800.0;

    public QuoteCalculateResponse calculate(
            QuoteCalculateRequest request) {

        String quoteReferenceId =
                request.getQuoteReferenceId().toString();
        log.info("Calculating quote for session: {}",
                quoteReferenceId);

        Map<String, Object> session =
                api1Client.fetchSession(quoteReferenceId);
        List<Map<String, Object>> drivers =
                api1Client.getDrivers(session);
        List<Map<String, Object>> vehicles =
                api1Client.getVehicles(session);
        String customerId = (String) session.get("customerId");
        String coverageTierId =
                (String) session.get("coverageTierId");
        Object deductibleObj = session.get("deductibleAmount");
        int deductibleAmount = deductibleObj instanceof Number
                ? ((Number) deductibleObj).intValue() : 500;

        if (vehicles.isEmpty()) {
            throw new RuntimeException(
                    "VALIDATION_ERROR: No vehicles found in session");
        }
        if (drivers.isEmpty()) {
            throw new RuntimeException(
                    "VALIDATION_ERROR: No drivers found in session");
        }

        Map<String, ClueVerificationEngine.ClueResult> clueResults
                = new LinkedHashMap<>();
        String overallRiskLevel = "CLEAN";
        for (Map<String, Object> driver : drivers) {
            String driverId = (String) driver.get("driverId");
            ClueVerificationEngine.ClueResult result =
                    clueEngine.verify(driver);
            clueResults.put(driverId, result);
            overallRiskLevel = worstRisk(
                    overallRiskLevel, result.getRiskLevel());
        }

        double averageAge = driverRiskEngine
                .computeAverageDriverAge(drivers);
        double driverAgeFactor = driverRiskEngine
                .computeDriverAgeFactor(averageAge);

        Map<String, Double> dlRecordFactors = new LinkedHashMap<>();
        for (Map<String, Object> driver : drivers) {
            String driverId = (String) driver.get("driverId");
            ClueVerificationEngine.ClueResult clue =
                    clueResults.get(driverId);
            double riskScore = driverRiskEngine.computeRiskScore(clue);
            double dlFactor = driverRiskEngine
                    .computeDlRecordFactor(riskScore);
            dlRecordFactors.put(driverId, dlFactor);
        }

        double priorFactor = priorInsuranceEngine
                .computeFactor(request.getPriorInsurance());

        double totalPremium = 0.0;
        List<VehicleBreakdown> breakdowns = new ArrayList<>();
        List<Discount> discounts = new ArrayList<>();

        for (Map<String, Object> vehicle : vehicles) {
            String vehicleId = (String) vehicle.get("vehicleId");
            String make = (String) vehicle.get("make");
            String model = (String) vehicle.get("model");
            Object yearObj = vehicle.get("year");
            int year = yearObj instanceof Number
                    ? ((Number) yearObj).intValue() : 2015;
            String primaryZip = (String) vehicle.get("primaryZip");
            String primaryDriverId =
                    (String) vehicle.get("primaryDriverId");

            double ageFactor = vehicleRatingEngine
                    .computeAgeFactor(year);
            double accidentFactor = vehicleRatingEngine
                    .computeVehicleAccidentFactor(make, model, year);
            double territoryFactor = vehicleRatingEngine
                    .computeTerritoryFactor(primaryZip);
            double dlFactor = dlRecordFactors
                    .getOrDefault(primaryDriverId, 1.0);

            double vehiclePremium = BASE_PREMIUM
                    * ageFactor
                    * accidentFactor
                    * territoryFactor
                    * dlFactor
                    * driverAgeFactor
                    * priorFactor;

            vehiclePremium = Math.round(
                    vehiclePremium * 100.0) / 100.0;
            totalPremium += vehiclePremium;

            VehicleBreakdown breakdown = new VehicleBreakdown();
            breakdown.setVehicleId(UUID.fromString(vehicleId));
            breakdown.setYear(year);
            breakdown.setMake(make);
            breakdown.setModel(model);
            breakdown.setBasePremium(BASE_PREMIUM);
            breakdown.setVehicleAgeFactor(ageFactor);
            breakdown.setVehicleAccidentFactor(accidentFactor);
            breakdown.setTerritoryFactor(territoryFactor);
            breakdown.setDlRecordFactor(dlFactor);
            breakdown.setDriverAgeFactor(driverAgeFactor);
            breakdown.setFinalPremium(vehiclePremium);
            breakdowns.add(breakdown);
        }

        boolean anyDefensiveCourse = drivers.stream()
                .anyMatch(d -> Boolean.TRUE.equals(
                        d.get("defensiveCourse")));
        if (anyDefensiveCourse) {
            double saving = Math.round(
                    totalPremium * 0.05 * 100.0) / 100.0;
            totalPremium = totalPremium * 0.95;
            Discount d = new Discount();
            d.setName("Defensive Driving Discount");
            d.setDescription(
                    "At least one driver completed a " +
                    "defensive driving course");
            d.setSavingsAmount(saving);
            discounts.add(d);
        }

        if (priorFactor == 0.95) {
            double saving = Math.round(
                    totalPremium * (1.0 / 0.95 - 1.0)
                    * 100.0) / 100.0;
            Discount d = new Discount();
            d.setName("Loyalty Discount");
            d.setDescription(
                    "3+ years continuous coverage with " +
                    "prior insurer");
            d.setSavingsAmount(saving);
            discounts.add(d);
        }

        totalPremium = Math.round(totalPremium * 100.0) / 100.0;

        String tier = coverageTierId != null
                ? coverageTierId : "CHOICE";
        List<QuotePackage> packages = buildPackages(
                totalPremium, deductibleAmount);

        String quoteId = UUID.randomUUID().toString();
        QuotePackage basicPkg = packages.get(0);
        QuotePackage choicePkg = packages.get(1);
        QuotePackage recommendedPkg = packages.get(2);

        String vehicleBreakdownJson = toJson(breakdowns);
        String discountsJson = toJson(discounts);
        String clueReportJson = toJson(buildClueReport(
                quoteReferenceId, clueResults, drivers,
                overallRiskLevel));

        QuoteEntity entity = QuoteEntity.builder()
                .quoteId(quoteId)
                .quoteReferenceId(quoteReferenceId)
                .customerId(customerId)
                .status("ACTIVE")
                .coverageTierId(tier)
                .deductibleAmount(deductibleAmount)
                .basicMonthly(basicPkg.getMonthlyPayment())
                .basicSixMonth(basicPkg.getSixMonthTotal())
                .basicPayInFull(basicPkg.getPayInFullAmount())
                .choiceMonthly(choicePkg.getMonthlyPayment())
                .choiceSixMonth(choicePkg.getSixMonthTotal())
                .choicePayInFull(choicePkg.getPayInFullAmount())
                .recommendedMonthly(
                        recommendedPkg.getMonthlyPayment())
                .recommendedSixMonth(
                        recommendedPkg.getSixMonthTotal())
                .recommendedPayInFull(
                        recommendedPkg.getPayInFullAmount())
                .vehicleBreakdownJson(vehicleBreakdownJson)
                .discountsAppliedJson(discountsJson)
                .clueReportJson(clueReportJson)
                .averageDriverAge(averageAge)
                .overallRiskLevel(overallRiskLevel)
                .priorInsuranceFactor(priorFactor)
                .createdAt(OffsetDateTime.now().toString())
                .expiresAt(OffsetDateTime.now()
                        .plusDays(30).toString())
                .build();

        quoteRepository.save(entity);
        log.info("Saved quote {} for session {}",
                quoteId, quoteReferenceId);

        QuoteCalculateResponse response =
                new QuoteCalculateResponse();
        response.setQuoteId(UUID.fromString(quoteId));
        response.setQuoteReferenceId(
                UUID.fromString(quoteReferenceId));
        response.setCustomerId(customerId != null
                ? UUID.fromString(customerId) : null);
        response.setStatus(
                QuoteCalculateResponse.StatusEnum.ACTIVE);
        response.setPackages(packages);
        response.setVehicleBreakdowns(breakdowns);
        response.setDiscountsApplied(discounts);
        response.setAverageDriverAge(averageAge);
        response.setClueRiskLevel(
                QuoteCalculateResponse.ClueRiskLevelEnum
                        .fromValue(overallRiskLevel));
        response.setCreatedAt(OffsetDateTime.now());
        response.setExpiresAt(
                OffsetDateTime.now().plusDays(30));
        return response;
    }

    public QuoteCalculateResponse getQuote(String quoteId) {
        QuoteEntity entity = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException(
                        "QUOTE_NOT_FOUND: " + quoteId));
        if (OffsetDateTime.parse(entity.getExpiresAt())
                .isBefore(OffsetDateTime.now())) {
            throw new RuntimeException(
                    "QUOTE_EXPIRED: " + quoteId);
        }
        return entityToResponse(entity);
    }

    public ClueReportResponse getClueReport(
            String quoteReferenceId) {
        QuoteEntity entity = quoteRepository
                .findByQuoteReferenceId(quoteReferenceId)
                .orElseThrow(() -> new RuntimeException(
                        "QUOTE_NOT_FOUND for session: "
                        + quoteReferenceId));
        try {
            return objectMapper.readValue(
                    entity.getClueReportJson(),
                    ClueReportResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not parse CLUE report");
        }
    }

    private List<QuotePackage> buildPackages(
            double totalPremium, int deductibleAmount) {
        List<QuotePackage> packages = new ArrayList<>();
        double[][] tiers = {{1.00}, {1.25}, {1.75}};
        String[] names = {"BASIC", "CHOICE", "RECOMMENDED"};
        String[] descs = {
            "Liability only — meets state minimum requirements",
            "Liability + collision coverage",
            "Full coverage — liability, collision and comprehensive"
        };
        for (int i = 0; i < 3; i++) {
            double sixMonth = Math.round(
                    totalPremium * tiers[i][0] * 100.0) / 100.0;
            double monthly = Math.round(
                    sixMonth / 6.0 * 100.0) / 100.0;
            double payInFull = Math.round(
                    sixMonth * 0.88 * 100.0) / 100.0;
            double savings = Math.round(
                    (sixMonth - payInFull) * 100.0) / 100.0;

            QuotePackage pkg = new QuotePackage();
            pkg.setTierName(QuotePackage.TierNameEnum
                    .fromValue(names[i]));
            pkg.setDescription(descs[i]);
            pkg.setSixMonthTotal(sixMonth);
            pkg.setMonthlyPayment(monthly);
            pkg.setPayInFullAmount(payInFull);
            pkg.setPayInFullSavings(savings);
            pkg.setDeductibleAmount(deductibleAmount);
            packages.add(pkg);
        }
        return packages;
    }

    private ClueReportResponse buildClueReport(
            String quoteReferenceId,
            Map<String, ClueVerificationEngine.ClueResult>
                    clueResults,
            List<Map<String, Object>> drivers,
            String overallRiskLevel) {

        ClueReportResponse report = new ClueReportResponse();
        report.setQuoteReferenceId(
                UUID.fromString(quoteReferenceId));
        report.setOverallRiskLevel(
                ClueReportResponse.OverallRiskLevelEnum
                        .fromValue(overallRiskLevel));

        List<DriverClueReport> driverReports = new ArrayList<>();
        for (Map<String, Object> driver : drivers) {
            String driverId = (String) driver.get("driverId");
            ClueVerificationEngine.ClueResult result =
                    clueResults.get(driverId);
            if (result == null) continue;

            String licenseNumber =
                    (String) driver.get("licenseNumber");
            String prefix = licenseNumber != null
                    && !licenseNumber.isBlank()
                    ? String.valueOf(licenseNumber
                            .toUpperCase().charAt(0))
                    : "?";

            DriverClueReport dr = new DriverClueReport();
            dr.setDriverId(UUID.fromString(driverId));
            dr.setFirstName((String) driver.get("firstName"));
            dr.setLastName((String) driver.get("lastName"));
            dr.setLicensePrefix(prefix);
            dr.setVerifiedAccidents(result.getVerifiedAccidents());
            dr.setVerifiedViolations(
                    result.isVerifiedViolations());
            dr.setSr22Override(result.isSr22Override());
            dr.setLicenseStatusOverride(
                    result.getLicenseStatusOverride());
            dr.setDiscrepancyFound(result.isDiscrepancyFound());
            dr.setDiscrepancyDetails(
                    result.getDiscrepancyDetails());
            dr.setRiskLevel(DriverClueReport.RiskLevelEnum
                    .fromValue(result.getRiskLevel()));
            driverReports.add(dr);
        }
        report.setDriverReports(driverReports);
        return report;
    }

    private QuoteCalculateResponse entityToResponse(
            QuoteEntity entity) {
        QuoteCalculateResponse response =
                new QuoteCalculateResponse();
        response.setQuoteId(
                UUID.fromString(entity.getQuoteId()));
        response.setQuoteReferenceId(UUID.fromString(
                entity.getQuoteReferenceId()));

        if (entity.getCustomerId() != null) {
            response.setCustomerId(
                    UUID.fromString(entity.getCustomerId()));
        }

        response.setStatus(
                QuoteCalculateResponse.StatusEnum.ACTIVE);
        response.setAverageDriverAge(
                entity.getAverageDriverAge());

        if (entity.getOverallRiskLevel() != null) {
            response.setClueRiskLevel(
                    QuoteCalculateResponse.ClueRiskLevelEnum
                            .fromValue(entity
                                    .getOverallRiskLevel()));
        }

        response.setCreatedAt(OffsetDateTime.parse(
                entity.getCreatedAt()));
        response.setExpiresAt(OffsetDateTime.parse(
                entity.getExpiresAt()));

        List<QuotePackage> packages = new ArrayList<>();
        String[] names = {"BASIC", "CHOICE", "RECOMMENDED"};
        String[] descs = {
            "Liability only — meets state minimum requirements",
            "Liability + collision coverage",
            "Full coverage — liability, collision and comprehensive"
        };
        double[] sixMonths = {
            entity.getBasicSixMonth(),
            entity.getChoiceSixMonth(),
            entity.getRecommendedSixMonth()
        };
        double[] monthlys = {
            entity.getBasicMonthly(),
            entity.getChoiceMonthly(),
            entity.getRecommendedMonthly()
        };
        double[] payInFulls = {
            entity.getBasicPayInFull(),
            entity.getChoicePayInFull(),
            entity.getRecommendedPayInFull()
        };
        for (int i = 0; i < 3; i++) {
            QuotePackage pkg = new QuotePackage();
            pkg.setTierName(QuotePackage.TierNameEnum
                    .fromValue(names[i]));
            pkg.setDescription(descs[i]);
            pkg.setSixMonthTotal(sixMonths[i]);
            pkg.setMonthlyPayment(monthlys[i]);
            pkg.setPayInFullAmount(payInFulls[i]);
            pkg.setPayInFullSavings(Math.round(
                    (sixMonths[i] - payInFulls[i])
                    * 100.0) / 100.0);
            pkg.setDeductibleAmount(
                    entity.getDeductibleAmount());
            packages.add(pkg);
        }
        response.setPackages(packages);

        if (entity.getVehicleBreakdownJson() != null) {
            try {
                List<VehicleBreakdown> breakdowns =
                        objectMapper.readValue(
                                entity.getVehicleBreakdownJson(),
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                VehicleBreakdown
                                                        .class));
                response.setVehicleBreakdowns(breakdowns);
            } catch (Exception e) {
                log.warn("Could not parse vehicleBreakdownJson");
            }
        }

        if (entity.getDiscountsAppliedJson() != null) {
            try {
                List<Discount> discounts =
                        objectMapper.readValue(
                                entity.getDiscountsAppliedJson(),
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                Discount.class));
                response.setDiscountsApplied(discounts);
            } catch (Exception e) {
                log.warn("Could not parse discountsAppliedJson");
            }
        }

        return response;
    }

    private String worstRisk(String current, String incoming) {
        List<String> order = List.of(
                "CLEAN", "MINOR", "MODERATE",
                "HIGH_RISK", "SUSPENDED");
        int a = order.indexOf(current);
        int b = order.indexOf(incoming);
        return order.get(Math.max(a, b));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}