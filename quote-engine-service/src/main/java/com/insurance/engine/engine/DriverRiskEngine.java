package com.insurance.engine.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DriverRiskEngine {

    public double computeAverageDriverAge(
            List<Map<String, Object>> drivers) {
        if (drivers == null || drivers.isEmpty()) return 35.0;
        double totalAge = 0;
        int count = 0;
        for (Map<String, Object> driver : drivers) {
            String dob = (String) driver.get("dateOfBirth");
            if (dob != null) {
                try {
                    LocalDate birthDate = LocalDate.parse(dob);
                    int age = Period.between(
                            birthDate, LocalDate.now()).getYears();
                    totalAge += age;
                    count++;
                } catch (Exception e) {
                    log.warn("Could not parse DOB: {}", dob);
                }
            }
        }
        return count > 0 ? totalAge / count : 35.0;
    }

    public double computeDriverAgeFactor(double averageAge) {
        if (averageAge < 25)  return 1.35;
        if (averageAge < 30)  return 1.15;
        if (averageAge <= 65) return 1.00;
        if (averageAge <= 75) return 1.10;
        return 1.25;
    }

    public double computeRiskScore(
            ClueVerificationEngine.ClueResult clueResult) {
        double score = 0.0;
        int accidents = clueResult.getVerifiedAccidents();
        if (accidents >= 1) score += 0.30;
        if (accidents >= 2) score += 0.20;
        if (accidents >= 3) score += 0.20;
        if (clueResult.isVerifiedViolations())  score += 0.20;
        if (clueResult.isSr22Override())        score += 0.40;
        if ("SUSPENDED".equals(
                clueResult.getLicenseStatusOverride())) score += 0.35;
        return Math.min(score, 1.0);
    }

    public double computeDlRecordFactor(double riskScore) {
        return Math.min(1.0 + (riskScore * 1.5), 2.5);
    }
}
