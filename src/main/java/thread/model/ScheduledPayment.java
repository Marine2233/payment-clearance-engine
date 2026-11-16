package thread.model;

import java.time.Duration;
import java.time.Instant;

public record ScheduledPayment(PaymentRequest request, Instant scheduleDate, Instant decidedAt) implements PaymentDecision {

    public ScheduledPayment{
        if (request == null){
            throw new IllegalArgumentException("request is null.");
        }

        if (scheduleDate == null || scheduleDate.isBefore(request.createdAtUtc())) {
            throw new IllegalArgumentException("local date is broken.");
        }

        if (decidedAt == null){
            throw new IllegalArgumentException("decided is null.");
        }
    }
}
