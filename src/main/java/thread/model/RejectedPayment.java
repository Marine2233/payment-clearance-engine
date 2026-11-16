package thread.model;

import thread.model.enums.ReasonReject;

import java.time.Instant;

public record RejectedPayment(PaymentRequest request, ReasonReject msgReject, Instant decidedAt) implements PaymentDecision {
    public RejectedPayment{

        if (request == null){
            throw new IllegalArgumentException("request is null.");
        }

        if (msgReject == null){
            throw new IllegalArgumentException("msg is null.");
        }

        if (decidedAt == null){
            throw new IllegalArgumentException("decided is null.");
        }
    }
}
