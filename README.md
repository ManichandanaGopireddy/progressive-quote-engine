# Progressive Quote Engine — Quote Calculation API

A production-grade Spring Boot microservice that calculates
auto insurance quotes using CLUE verification, real NHTSA
vehicle safety data, and multi-factor rate calculation.

Built as a completely independent microservice — communicates
with API 1 (Quote Session Service) only over HTTP.

---

## Architecture

This is **API 2** of a two-microservice insurance platform.
progressive-quote-engine/
├── openapi-contract/       # YAML contract + generated Java interfaces
└── quote-engine-service/   # Spring Boot application (port 8082)

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Language |
| Spring Boot 3.2.4 | Application framework |
| Spring WebFlux | WebClient for inter-service HTTP calls |
| OpenAPI Generator 7.4.0 | Contract-first code generation |
| AWS DynamoDB | Quote persistence |
| JavaMailSender | Email delivery via Gmail SMTP |
| Lombok | Boilerplate reduction |
| Springdoc OpenAPI | Swagger UI |

---

## DynamoDB Tables

| Table | Partition Key | Purpose |
|---|---|---|
| quotes | quoteId | Calculated quote packages |

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v2/quotes/coverage-options | Available tiers and deductibles |
| POST | /api/v2/quotes/calculate | Calculate quote from session data |
| GET | /api/v2/quotes/{quoteId} | Retrieve saved quote |
| POST | /api/v2/quotes/{quoteId}/email | Email quote to customer |
| GET | /api/v2/quotes/clue/{quoteReferenceId} | CLUE verification report |

---

## Rate Calculation Formula
vehiclePremium = $800
× vehicleAgeFactor        (vehicle year — 0.90 to 1.20)
× vehicleAccidentFactor   (NHTSA data — 0.90 to 1.18)
× territoryFactor         (ZIP state — 1.00 to 1.35)
× dlRecordFactor          (CLUE verified — 1.00 to 2.50)
× driverAgeFactor         (average age — 1.00 to 1.35)
× priorInsuranceFactor    (history — 0.95 to 1.25)
totalPremium = sum of all vehicle premiums
BASIC       = totalPremium × 1.00
CHOICE      = totalPremium × 1.25
RECOMMENDED = totalPremium × 1.75
monthlyPayment = sixMonthTotal ÷ 6
payInFull      = sixMonthTotal × 0.88 (12% discount)

---

## CLUE Verification Engine

Uses the first character of the driver's license number
to simulate CLUE (Comprehensive Loss Underwriting Exchange)
verification. Rules defined in `clue-rules.properties`.

| License Prefix | Risk Level | Effect |
|---|---|---|
| 1, 4, 7, A-N | CLEAN | Customer data used as-is |
| 2, 3, O-S | MINOR | +1 accident added |
| 5, 8, T-X | MODERATE | +2 accidents, violations=true |
| 9, 6, Y, Z | HIGH_RISK | +3 accidents, SR22=true |
| 0 | SUSPENDED | licenseStatus overridden |

---

## Vehicle Accident Data

Real NHTSA ODI complaint data for 129 vehicle models
from 2010 to 2024. Sourced from NHTSA official complaints
database (1,538 records). Loaded at startup from
`vehicle-accident-data.csv`.

---

## Prerequisites

- Java 21
- Maven 3.8+
- AWS account with DynamoDB access
- AWS credentials at `~/.aws/credentials`
- Gmail account with App Password for email delivery
- API 1 (progressive-insurance-mock) running on port 8081

---

## Configuration

Update `application.yml`:

```yaml
spring:
  mail:
    username: your-gmail@gmail.com
    password: your-16-char-app-password

api1:
  base-url: http://localhost:8081
```

---

## Running the Application

```bash
# Build
mvn clean install -DskipTests

# Run (API 1 must be running first)
java -jar quote-engine-service/target/quote-engine-service-1.0.0-SNAPSHOT.jar
```

Application starts on **port 8082**.

Swagger UI: `http://localhost:8082/swagger-ui/index.html`

---

## Test Flow

GET  /api/v2/quotes/coverage-options
→ View available tiers and deductibles
POST /api/v2/quotes/calculate
→ Pass quoteReferenceId from API 1
→ Returns quoteId + 3 packages with pricing
GET  /api/v2/quotes/{quoteId}
→ Retrieve saved quote
POST /api/v2/quotes/{quoteId}/email
→ Send quote to customer email
GET  /api/v2/quotes/clue/{quoteReferenceId}
→ View CLUE verification results per driver


---

## Environment Setup

Before running, update `application.yml` with your own credentials:

```yaml
spring:
  mail:
    username: your-gmail@gmail.com      # Your Gmail address
    password: your-app-password-here    # Gmail App Password (16 chars)
```

To generate a Gmail App Password:
1. Go to https://myaccount.google.com/security
2. Enable 2-Step Verification
3. Search for "App passwords"
4. Create a new app password and paste it here

## Related Project

**API 1 — Progressive Insurance Mock**
Handles customer data collection — identity, drivers,
vehicles, and coverage selection.

Repository: `progressive-insurance-mock`
