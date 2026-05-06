package com.insurance.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoDbTableInitializer implements ApplicationRunner {

    private final DynamoDbClient dynamoDbClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Initializing DynamoDB tables...");
            createTableIfNotExists("quotes", "quoteId", null);
            log.info("DynamoDB table initialization complete.");
        } catch (Exception e) {
            log.warn("DynamoDB table initialization failed: {}",
                    e.getMessage());
        }
    }

    private void createTableIfNotExists(
            String tableName, String partitionKey, String sortKey) {
        try {
            dynamoDbClient.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(tableName).build());
            log.info("Table '{}' already exists - skipping.", tableName);
        } catch (ResourceNotFoundException e) {
            log.info("Creating table '{}'...", tableName);
            var attributeDefinitions = new ArrayList<AttributeDefinition>();
            var keySchema = new ArrayList<KeySchemaElement>();
            attributeDefinitions.add(AttributeDefinition.builder()
                    .attributeName(partitionKey)
                    .attributeType(ScalarAttributeType.S).build());
            keySchema.add(KeySchemaElement.builder()
                    .attributeName(partitionKey)
                    .keyType(KeyType.HASH).build());
            dynamoDbClient.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(attributeDefinitions)
                    .keySchema(keySchema)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            waitUntilActive(tableName);
            log.info("Table '{}' created successfully.", tableName);
        }
    }

    private void waitUntilActive(String tableName) {
        int attempts = 0;
        while (attempts < 20) {
            try {
                Thread.sleep(2000);
                DescribeTableResponse response =
                        dynamoDbClient.describeTable(
                                DescribeTableRequest.builder()
                                        .tableName(tableName).build());
                if (response.table().tableStatus()
                        == TableStatus.ACTIVE) return;
            } catch (Exception ex) {
                return;
            }
            attempts++;
        }
    }
}