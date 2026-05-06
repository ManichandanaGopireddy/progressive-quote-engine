package com.insurance.engine.repository.impl;

import com.insurance.engine.entity.QuoteEntity;
import com.insurance.engine.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class QuoteRepositoryImpl implements QuoteRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<QuoteEntity> table() {
        return enhancedClient.table("quotes",
                TableSchema.fromBean(QuoteEntity.class));
    }

    @Override
    public QuoteEntity save(QuoteEntity entity) {
        try {
            table().putItem(entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save quote", e);
        }
    }

    @Override
    public Optional<QuoteEntity> findById(String quoteId) {
        try {
            Key key = Key.builder()
                    .partitionValue(quoteId)
                    .build();
            return Optional.ofNullable(table().getItem(key));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find quote: " + quoteId, e);
        }
    }

    @Override
    public Optional<QuoteEntity> findByQuoteReferenceId(
            String quoteReferenceId) {
        try {
            ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                    .filterExpression(Expression.builder()
                            .expression("quoteReferenceId = :qrid")
                            .expressionValues(Map.of(":qrid",
                                    AttributeValue.builder()
                                            .s(quoteReferenceId)
                                            .build()))
                            .build())
                    .build();
            return table().scan(request)
                    .items()
                    .stream()
                    .filter(q -> "ACTIVE".equals(q.getStatus()))
                    .findFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find quote by reference: "
                    + quoteReferenceId, e);
        }
    }
}
