package com.insurance.engine.engine;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class VehicleRatingEngine {

    // Key: make|model|year  Value: riskFactor
    private final Map<String, Double> vehicleFactorMap = new HashMap<>();

    // Key: make|model  Value: average riskFactor (fallback)
    private final Map<String, Double> vehicleFallbackMap = new HashMap<>();
    private final Map<String, Integer> vehicleFallbackCount = new HashMap<>();

    @PostConstruct
    public void loadVehicleData() {
        try {
            ClassPathResource resource = new ClassPathResource(
                    "vehicle-accident-data.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()));
            String line;
            boolean firstLine = true;
            int loaded = 0;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String make  = parts[0].trim().toLowerCase();
                    String model = parts[1].trim().toLowerCase();
                    String year  = parts[2].trim();
                    double factor = Double.parseDouble(parts[4].trim());

                    // Exact key: make|model|year
                    String exactKey = make + "|" + model + "|" + year;
                    vehicleFactorMap.put(exactKey, factor);

                    // Fallback key: make|model (running average)
                    String fallbackKey = make + "|" + model;
                    double current = vehicleFallbackMap
                            .getOrDefault(fallbackKey, 0.0);
                    int count = vehicleFallbackCount
                            .getOrDefault(fallbackKey, 0);
                    vehicleFallbackMap.put(fallbackKey,
                            (current * count + factor) / (count + 1));
                    vehicleFallbackCount.put(fallbackKey, count + 1);

                    loaded++;
                }
            }
            reader.close();
            log.info("Loaded {} vehicle risk records ({} unique make/model " +
                    "combinations) from real NHTSA data",
                    loaded, vehicleFallbackMap.size());
        } catch (Exception e) {
            log.warn("Could not load vehicle accident data: {}",
                    e.getMessage());
        }
    }

    public double computeVehicleAccidentFactor(
            String make, String model, int year) {
        if (make == null || model == null) return 1.00;

        String makeLower  = make.trim().toLowerCase();
        String modelLower = model.trim().toLowerCase();

        // Try exact match: make|model|year
        String exactKey = makeLower + "|" + modelLower + "|" + year;
        if (vehicleFactorMap.containsKey(exactKey)) {
            double factor = vehicleFactorMap.get(exactKey);
            log.debug("Exact NHTSA match: {} {} {} → factor {}",
                    make, model, year, factor);
            return factor;
        }

        // Fallback: make|model average across all years
        String fallbackKey = makeLower + "|" + modelLower;
        if (vehicleFallbackMap.containsKey(fallbackKey)) {
            double factor = Math.round(
                    vehicleFallbackMap.get(fallbackKey) * 100.0) / 100.0;
            log.debug("Fallback NHTSA match: {} {} (any year) → factor {}",
                    make, model, factor);
            return factor;
        }

        // Default — car not found in NHTSA data
        log.warn("No NHTSA data for {} {} {} — using default 1.00",
                make, model, year);
        return 1.00;
    }

    public double computeAgeFactor(int vehicleYear) {
        int currentYear = LocalDate.now().getYear();
        int age = currentYear - vehicleYear;
        if (age <= 2)  return 0.90;
        if (age <= 5)  return 0.95;
        if (age <= 10) return 1.00;
        if (age <= 15) return 1.10;
        return 1.20;
    }

    public double computeTerritoryFactor(String primaryZip) {
        if (primaryZip == null || primaryZip.length() < 3)
            return 1.00;
        try {
            int zipNum = Integer.parseInt(
                    primaryZip.trim().substring(0, 3));
            if (zipNum >= 300 && zipNum <= 319) return 1.10; // GA
            if (zipNum >= 100 && zipNum <= 149) return 1.35; // NY
            if (zipNum >= 900 && zipNum <= 961) return 1.25; // CA
            if (zipNum >= 750 && zipNum <= 799) return 1.15; // TX
            if (zipNum >= 320 && zipNum <= 349) return 1.20; // FL
            if (zipNum >= 600 && zipNum <= 629) return 1.15; // IL
            if (zipNum >= 150 && zipNum <= 196) return 1.10; // PA
            return 1.00;
        } catch (Exception e) {
            return 1.00;
        }
    }
}