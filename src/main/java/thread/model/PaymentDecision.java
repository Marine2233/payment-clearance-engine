package thread.model;

import java.math.BigInteger;
import java.time.Instant;

public sealed interface PaymentDecision extends Identifiable<BigInteger> permits ApprovedPayment, ScheduledPayment, RejectedPayment  {

    PaymentRequest request();

    //решено в
    Instant decidedAt();

    @Override
    default BigInteger getId() {
        return request().id();
    }
}
