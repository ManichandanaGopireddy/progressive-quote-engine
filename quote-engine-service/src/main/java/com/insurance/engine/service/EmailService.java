package com.insurance.engine.service;

import com.insurance.engine.api.model.QuotePackage;
import com.insurance.engine.api.model.Discount;
import com.insurance.engine.entity.QuoteEntity;
import com.insurance.engine.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final QuoteRepository quoteRepository;
    private final QuoteService quoteService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public String sendQuoteEmail(
            String quoteId, String recipientEmail) {

        QuoteEntity entity = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException(
                        "QUOTE_NOT_FOUND: " + quoteId));

        String toEmail = (recipientEmail != null
                && !recipientEmail.isBlank())
                ? recipientEmail
                : "customer@example.com";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(
                    "Your Progressive Insurance Quote — " +
                    "Valid until " +
                    formatDate(entity.getExpiresAt()));

            String html = buildHtml(entity);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Quote email sent to {} for quote {}",
                    toEmail, quoteId);
            return toEmail;

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}",
                    toEmail, e.getMessage());
            throw new RuntimeException(
                    "EMAIL_FAILED: " + e.getMessage());
        }
    }

    private String buildHtml(QuoteEntity entity) {
        String expiresAt = formatDate(entity.getExpiresAt());

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
  body { font-family: Arial, sans-serif; background: #f4f4f4;
         margin: 0; padding: 20px; }
  .container { max-width: 650px; margin: 0 auto;
               background: white; border-radius: 8px;
               overflow: hidden;
               box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
  .header { background: #003087; color: white;
             padding: 30px; text-align: center; }
  .header h1 { margin: 0; font-size: 24px; }
  .header p  { margin: 8px 0 0; opacity: 0.85; font-size: 14px; }
  .body { padding: 30px; }
  .packages { display: flex; gap: 16px; margin: 20px 0; }
  .pkg { flex: 1; border: 2px solid #e0e0e0;
          border-radius: 8px; padding: 16px;
          text-align: center; }
  .pkg.recommended { border-color: #003087;
                      background: #f0f4ff; }
  .pkg-name { font-size: 13px; font-weight: bold;
               text-transform: uppercase;
               color: #003087; margin-bottom: 8px; }
  .pkg-desc { font-size: 11px; color: #666;
               margin-bottom: 12px; min-height: 32px; }
  .pkg-price { font-size: 28px; font-weight: bold;
                color: #003087; }
  .pkg-period { font-size: 12px; color: #666; }
  .pkg-pif { font-size: 12px; color: #2e7d32;
              margin-top: 6px; }
  .pkg-savings { font-size: 11px; color: #2e7d32; }
  .badge { background: #003087; color: white;
            font-size: 10px; padding: 2px 8px;
            border-radius: 10px; display: inline-block;
            margin-bottom: 8px; }
  .discounts { background: #f0fff4; border: 1px solid #c8e6c9;
                border-radius: 6px; padding: 16px;
                margin: 16px 0; }
  .discounts h3 { margin: 0 0 10px; color: #2e7d32;
                   font-size: 14px; }
  .discount-item { display: flex; justify-content: space-between;
                    font-size: 13px; padding: 4px 0; }
  .footer { background: #f8f8f8; padding: 20px;
             text-align: center; font-size: 12px;
             color: #999; border-top: 1px solid #eee; }
  .expiry { background: #fff8e1; border: 1px solid #ffe082;
             border-radius: 6px; padding: 12px;
             text-align: center; margin: 16px 0;
             font-size: 13px; color: #795548; }
</style>
</head>
<body>
<div class="container">
  <div class="header">
    <h1>Your Insurance Quote</h1>
    <p>Quote Reference: %s</p>
  </div>
  <div class="body">
    <p>Thank you for choosing Progressive Insurance.
    Your personalized quote is ready.</p>

    <div class="packages">
      <div class="pkg">
        <div class="pkg-name">Basic</div>
        <div class="pkg-desc">Liability only. Meets
        state minimum requirements.</div>
        <div class="pkg-price">$%s</div>
        <div class="pkg-period">per month</div>
        <div class="pkg-pif">Pay in full: $%s</div>
        <div class="pkg-savings">Save $%s</div>
      </div>
      <div class="pkg recommended">
        <div class="badge">Most Popular</div>
        <div class="pkg-name">Choice</div>
        <div class="pkg-desc">Liability + collision
        coverage.</div>
        <div class="pkg-price">$%s</div>
        <div class="pkg-period">per month</div>
        <div class="pkg-pif">Pay in full: $%s</div>
        <div class="pkg-savings">Save $%s</div>
      </div>
      <div class="pkg">
        <div class="pkg-name">Recommended</div>
        <div class="pkg-desc">Full coverage. Maximum
        protection.</div>
        <div class="pkg-price">$%s</div>
        <div class="pkg-period">per month</div>
        <div class="pkg-pif">Pay in full: $%s</div>
        <div class="pkg-savings">Save $%s</div>
      </div>
    </div>

    %s

    <div class="expiry">
      ⏰ This quote is valid until <strong>%s</strong>.
      After this date you will need to request a new quote.
    </div>

    <p style="font-size:13px;color:#666;">
      Your quote was calculated using verified driving history
      and real vehicle safety data. All prices are for a
      6-month policy period.
    </p>
  </div>
  <div class="footer">
    Progressive Insurance Mock Platform<br>
    This is a demonstration system. Not a real insurance offer.
  </div>
</div>
</body>
</html>
""".formatted(
            entity.getQuoteReferenceId(),
            fmt(entity.getBasicMonthly()),
            fmt(entity.getBasicPayInFull()),
            fmt(entity.getBasicSixMonth()
                    - entity.getBasicPayInFull()),
            fmt(entity.getChoiceMonthly()),
            fmt(entity.getChoicePayInFull()),
            fmt(entity.getChoiceSixMonth()
                    - entity.getChoicePayInFull()),
            fmt(entity.getRecommendedMonthly()),
            fmt(entity.getRecommendedPayInFull()),
            fmt(entity.getRecommendedSixMonth()
                    - entity.getRecommendedPayInFull()),
            buildDiscountsHtml(entity),
            expiresAt
        );
    }

    private String buildDiscountsHtml(QuoteEntity entity) {
        if (entity.getDiscountsAppliedJson() == null
                || entity.getDiscountsAppliedJson()
                        .equals("[]")) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <div class="discounts">
              <h3>✓ Discounts Applied</h3>
            """);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind
                            .ObjectMapper();
            List<com.insurance.engine.api.model.Discount>
                    discounts = mapper.readValue(
                            entity.getDiscountsAppliedJson(),
                            mapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            Discount.class));
            for (Discount d : discounts) {
                sb.append("""
                    <div class="discount-item">
                      <span>%s</span>
                      <span style="color:#2e7d32;
                                   font-weight:bold;">
                        -$%s
                      </span>
                    </div>
                    """.formatted(
                        d.getName(),
                        fmt(d.getSavingsAmount())));
            }
        } catch (Exception e) {
            log.warn("Could not parse discounts for email");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String fmt(Double value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }

    private String formatDate(String dateStr) {
        try {
            return OffsetDateTime.parse(dateStr)
                    .format(DateTimeFormatter
                            .ofPattern("MMMM dd, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}