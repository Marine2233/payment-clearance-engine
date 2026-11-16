package thread.service;

import thread.model.Client;
import thread.model.PaymentRequest;
import thread.model.enums.PaymentCategory;
import thread.model.enums.Rate;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class CommissionCalculator {

    public BigDecimal calculate(PaymentRequest payment, Client client) {

        if (payment == null) {
            throw new IllegalArgumentException("Невалидный параметр поручения.");
        }

        if (client == null) {
            throw new IllegalArgumentException("Клиент не существует.");
        }

        if (payment.clientId() != client.id()) {
            throw new IllegalArgumentException("Broken client/payment.");
        }

        if (client.rate().equals(Rate.BASIC) && payment.category().equals(PaymentCategory.SERVICE)) {
            BigDecimal pay = payment.amount();
            BigDecimal commission = pay.multiply(BigDecimal.valueOf(0.02));
            commission = commission.setScale(2, RoundingMode.HALF_EVEN);
            return commission;
        }

        if (client.rate().equals(Rate.BASIC) && payment.category().equals(PaymentCategory.TRANSFER)) {
            BigDecimal pay = payment.amount();
            BigDecimal commission = pay.multiply(BigDecimal.valueOf(0.01));
            commission = commission.setScale(2, RoundingMode.HALF_EVEN);
            return commission;
        }

        if (client.rate().equals(Rate.BASIC) && payment.category().equals(PaymentCategory.UTILITY)) {
            return BigDecimal.ZERO.setScale(2,RoundingMode.HALF_EVEN);
        }

        if (client.rate().equals(Rate.PREMIUM) && payment.category().equals(PaymentCategory.SERVICE)) {
            BigDecimal pay = payment.amount();
            BigDecimal commission = pay.multiply(BigDecimal.valueOf(0.01));
            commission = commission.setScale(2, RoundingMode.HALF_EVEN);
            return commission;
        }

        if (client.rate().equals(Rate.PREMIUM) && payment.category().equals(PaymentCategory.TRANSFER)) {
            BigDecimal pay = payment.amount();
            BigDecimal commission = pay.multiply(BigDecimal.valueOf(0.005));
            commission = commission.setScale(2, RoundingMode.HALF_EVEN);
            return commission;
        }

        if (client.rate().equals(Rate.PREMIUM) && payment.category().equals(PaymentCategory.UTILITY)) {
            return BigDecimal.ZERO.setScale(2,RoundingMode.HALF_EVEN);
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal getDailyLimit(Rate tariff){

        if (tariff == null){
            throw new IllegalArgumentException("Not valid data.");
        }

        return switch (tariff) {
            case BASIC -> new BigDecimal("5000.00").setScale(2,RoundingMode.HALF_EVEN);
            case PREMIUM -> new BigDecimal("20000.00").setScale(2,RoundingMode.HALF_EVEN);
        };
    }
}
