package thread.model;

import thread.model.enums.PaymentCategory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;

public record PaymentRequest(BigInteger id,
                             long clientId,
                             BigDecimal amount,
                             PaymentCategory category,
                             Instant createdAtUtc,
                             LocalDateTime scheduledLocalTime)implements Identifiable<BigInteger> {

    public PaymentRequest{
        if (id == null || id.compareTo(BigInteger.ZERO) <= 0){
            throw new IllegalArgumentException("id поручения обязательно должен быть положительным");
        }
        if (clientId <= 0){
            throw new IllegalArgumentException("id клиента должен быть строго положительным.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 ){
            throw new IllegalArgumentException("Платежное поручение должно быть больше 0.");
        }
        if (category == null){
            throw new IllegalArgumentException("unknown category payment");
        }
        if (createdAtUtc == null || scheduledLocalTime == null){
            throw new IllegalArgumentException("Даты не заданы.");
        }

        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public BigInteger getId() {
        return id;
    }
}
