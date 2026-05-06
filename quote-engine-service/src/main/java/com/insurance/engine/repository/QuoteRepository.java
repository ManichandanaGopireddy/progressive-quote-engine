package com.insurance.engine.repository;

import com.insurance.engine.entity.QuoteEntity;
import java.util.Optional;

public interface QuoteRepository {
    QuoteEntity save(QuoteEntity entity);
    Optional<QuoteEntity> findById(String quoteId);
    Optional<QuoteEntity> findByQuoteReferenceId(String quoteReferenceId);
}