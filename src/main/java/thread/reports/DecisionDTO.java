package thread.reports;

import java.math.BigInteger;
import java.time.Instant;

public record DecisionDTO(BigInteger paymentId,
                          long clientId,
                          String amount,
                          String status,
                          String commission,
                          String rejectReason,
                          Instant scheduledAtUtc) {
}
