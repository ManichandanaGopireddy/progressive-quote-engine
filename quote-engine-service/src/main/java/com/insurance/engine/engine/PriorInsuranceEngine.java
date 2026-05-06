package com.insurance.engine.engine;

import com.insurance.engine.api.model.PriorInsurance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PriorInsuranceEngine {

    public double computeFactor(PriorInsurance priorInsurance) {
        if (priorInsurance == null) return 1.00;

        if (!Boolean.TRUE.equals(
                priorInsurance.getHasPriorInsurance())) {
            log.info("No prior insurance — applying surcharge 1.25");
            return 1.25;
        }

        if (Boolean.TRUE.equals(
                priorInsurance.getLapseInCoverage())) {
            log.info("Coverage lapse found — applying surcharge 1.15");
            return 1.15;
        }

        int years = priorInsurance.getYearsWithPriorInsurer() != null
                ? priorInsurance.getYearsWithPriorInsurer() : 0;

        if (years >= 3) {
            log.info("3+ years continuous coverage — loyalty discount 0.95");
            return 0.95;
        }

        return 1.00;
    }
}