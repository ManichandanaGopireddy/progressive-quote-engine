package com.insurance.engine.controller;

import com.insurance.engine.api.CoverageApi;
import com.insurance.engine.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class CoverageController implements CoverageApi {

    @Override
    public ResponseEntity<CoverageOptionsResponse>
            getCoverageOptions() {
        log.info("GET /api/v2/quotes/coverage-options");

        CoverageTier basic = new CoverageTier();
        basic.setTierId(CoverageTier.TierIdEnum.BASIC);
        basic.setName("Basic");
        basic.setDescription(
                "Liability only. Meets state minimum requirements. " +
                "Covers damage you cause to others.");
        basic.setMultiplier(1.00);

        CoverageTier choice = new CoverageTier();
        choice.setTierId(CoverageTier.TierIdEnum.CHOICE);
        choice.setName("Choice");
        choice.setDescription(
                "Liability + collision. Covers damage to your vehicle " +
                "in an accident plus others.");
        choice.setMultiplier(1.25);

        CoverageTier recommended = new CoverageTier();
        recommended.setTierId(CoverageTier.TierIdEnum.RECOMMENDED);
        recommended.setName("Recommended");
        recommended.setDescription(
                "Full coverage. Liability, collision and comprehensive. " +
                "Maximum protection.");
        recommended.setMultiplier(1.75);

        DeductibleOption d250 = new DeductibleOption();
        d250.setAmount(250);
        d250.setDescription("$250 deductible — lower out of pocket, " +
                "higher premium");

        DeductibleOption d500 = new DeductibleOption();
        d500.setAmount(500);
        d500.setDescription("$500 deductible — standard option");

        DeductibleOption d1000 = new DeductibleOption();
        d1000.setAmount(1000);
        d1000.setDescription("$1000 deductible — lower premium, " +
                "higher out of pocket");

        DeductibleOption d2000 = new DeductibleOption();
        d2000.setAmount(2000);
        d2000.setDescription("$2000 deductible — lowest premium, " +
                "highest out of pocket");

        CoverageOptionsResponse response =
                new CoverageOptionsResponse();
        response.setTiers(List.of(basic, choice, recommended));
        response.setDeductibles(List.of(d250, d500, d1000, d2000));
        return ResponseEntity.ok(response);
    }
}