package thread.reports;

import java.time.Instant;
import java.util.List;

public record DecisionReportDto (Instant generatedAtUtc,
                                 SummaryDTO summary,
                                 List<DecisionDTO> decisions){
}
