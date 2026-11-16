package thread.reports;

public record SummaryDTO (int totalChecked,
                          int approvedCount,
                          int scheduledCount,
                          int rejectedCount,
                          int importErrorsCount,
                          String totalApprovedAmount,
                          String totalCommissionAmount){
}
