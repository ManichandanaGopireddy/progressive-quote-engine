package com.insurance.engine.controller;

import com.insurance.engine.api.QuoteApi;
import com.insurance.engine.api.model.*;
import com.insurance.engine.service.EmailService;
import com.insurance.engine.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuoteController implements QuoteApi {

    private final QuoteService quoteService;
    private final EmailService emailService;

    @Override
    public ResponseEntity<QuoteCalculateResponse> calculateQuote(
            QuoteCalculateRequest request) {
        log.info("POST /api/v2/quotes/calculate for session: {}",
                request.getQuoteReferenceId());
        return ResponseEntity.status(201)
                .body(quoteService.calculate(request));
    }

    @Override
    public ResponseEntity<QuoteCalculateResponse> getQuote(
            UUID quoteId) {
        log.info("GET /api/v2/quotes/{}", quoteId);
        return ResponseEntity.ok(
                quoteService.getQuote(quoteId.toString()));
    }

    @Override
    public ResponseEntity<QuoteEmailResponse> emailQuote(
            UUID quoteId,
            QuoteEmailRequest request) {
        log.info("POST /api/v2/quotes/{}/email", quoteId);
        String recipient = (request != null
                && request.getRecipientEmail() != null)
                ? request.getRecipientEmail()
                : null;
        String sentTo = emailService.sendQuoteEmail(
                quoteId.toString(), recipient);
        QuoteEmailResponse response = new QuoteEmailResponse();
        response.setSent(true);
        response.setSentTo(sentTo);
        response.setMessage("Quote email sent successfully");
        return ResponseEntity.ok(response);
    }
}