package com.insurance.engine.controller;

import com.insurance.engine.api.ClueApi;
import com.insurance.engine.api.model.ClueReportResponse;
import com.insurance.engine.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClueController implements ClueApi {

    private final QuoteService quoteService;

    @Override
    public ResponseEntity<ClueReportResponse> getClueReport(
            UUID quoteReferenceId) {
        log.info("GET /api/v2/quotes/clue/{}", quoteReferenceId);
        return ResponseEntity.ok(
                quoteService.getClueReport(
                        quoteReferenceId.toString()));
    }
}