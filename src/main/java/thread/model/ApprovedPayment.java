package thread.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

public record ApprovedPayment(PaymentRequest request, BigDecimal commission, LocalDate dateBank,Instant decidedAt) implements PaymentDecision {
   public ApprovedPayment{

       if (request == null){
           throw new IllegalArgumentException("Request is null.");
       }

       if (commission == null || commission.compareTo(BigDecimal.ZERO) < 0){
           throw new IllegalArgumentException("Commission is null.");
       }

       if (dateBank == null){
           throw new IllegalArgumentException("date is null.");
       }

       if (decidedAt == null){
           throw new IllegalArgumentException("decide is null.");
       }

       commission = commission.setScale(2, RoundingMode.HALF_EVEN);
   }
}
