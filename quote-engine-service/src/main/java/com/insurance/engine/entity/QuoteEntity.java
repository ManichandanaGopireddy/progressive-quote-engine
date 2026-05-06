package com.insurance.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class QuoteEntity {

    private String quoteId;
    private String quoteReferenceId;
    private String customerId;
    private String status;
    private String coverageTierId;
    private Integer deductibleAmount;
    private Double basicMonthly;
    private Double basicSixMonth;
    private Double basicPayInFull;
    private Double choiceMonthly;
    private Double choiceSixMonth;
    private Double choicePayInFull;
    private Double recommendedMonthly;
    private Double recommendedSixMonth;
    private Double recommendedPayInFull;
    private String vehicleBreakdownJson;
    private String discountsAppliedJson;
    private String clueReportJson;
    private Double averageDriverAge;
    private String overallRiskLevel;
    private Double priorInsuranceFactor;
    private String createdAt;
    private String expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("quoteId")
    public String getQuoteId() { return quoteId; }

    @DynamoDbAttribute("quoteReferenceId")
    public String getQuoteReferenceId() { return quoteReferenceId; }

    @DynamoDbAttribute("customerId")
    public String getCustomerId() { return customerId; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }

    @DynamoDbAttribute("coverageTierId")
    public String getCoverageTierId() { return coverageTierId; }

    @DynamoDbAttribute("deductibleAmount")
    public Integer getDeductibleAmount() { return deductibleAmount; }

    @DynamoDbAttribute("basicMonthly")
    public Double getBasicMonthly() { return basicMonthly; }

    @DynamoDbAttribute("basicSixMonth")
    public Double getBasicSixMonth() { return basicSixMonth; }

    @DynamoDbAttribute("basicPayInFull")
    public Double getBasicPayInFull() { return basicPayInFull; }

    @DynamoDbAttribute("choiceMonthly")
    public Double getChoiceMonthly() { return choiceMonthly; }

    @DynamoDbAttribute("choiceSixMonth")
    public Double getChoiceSixMonth() { return choiceSixMonth; }

    @DynamoDbAttribute("choicePayInFull")
    public Double getChoicePayInFull() { return choicePayInFull; }

    @DynamoDbAttribute("recommendedMonthly")
    public Double getRecommendedMonthly() { return recommendedMonthly; }

    @DynamoDbAttribute("recommendedSixMonth")
    public Double getRecommendedSixMonth() { return recommendedSixMonth; }

    @DynamoDbAttribute("recommendedPayInFull")
    public Double getRecommendedPayInFull() { return recommendedPayInFull; }

    @DynamoDbAttribute("vehicleBreakdownJson")
    public String getVehicleBreakdownJson() { return vehicleBreakdownJson; }

    @DynamoDbAttribute("discountsAppliedJson")
    public String getDiscountsAppliedJson() { return discountsAppliedJson; }

    @DynamoDbAttribute("clueReportJson")
    public String getClueReportJson() { return clueReportJson; }

    @DynamoDbAttribute("averageDriverAge")
    public Double getAverageDriverAge() { return averageDriverAge; }

    @DynamoDbAttribute("overallRiskLevel")
    public String getOverallRiskLevel() { return overallRiskLevel; }

    @DynamoDbAttribute("priorInsuranceFactor")
    public Double getPriorInsuranceFactor() { return priorInsuranceFactor; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }

    @DynamoDbAttribute("expiresAt")
    public String getExpiresAt() { return expiresAt; }
}