package thread.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import thread.importer.ErrorResult;
import thread.model.*;
import thread.reports.DecisionDTO;
import thread.reports.DecisionReportDto;
import thread.reports.SummaryDTO;
import thread.repository.Repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportGeneratorService {

    private final ObjectMapper mapper;
    private final Clock clock;
    public ReportGeneratorService(Clock clock){
        if (clock == null){
            throw new IllegalArgumentException("Clock не может быть null.");
        }

        this.clock = clock;
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    public void writeDecisionsJson(
            Path outputDir,
            Repository<BigInteger, PaymentDecision> repository,
            Map<Long, Client> clients,
            List<ErrorResult> clientErrors,
            List<ErrorResult> paymentErrors
    ) {
        validateClientsMap(clients);

        try {
            Files.createDirectories(outputDir);

            List<PaymentDecision> decisions = getSortedDecisions(repository);
            SummaryDTO summary = calculateStats(decisions, clientErrors, paymentErrors);
            List<DecisionDTO> items = decisions.stream()
                    .map(d -> toDto(d, clients.get(d.request().clientId())))
                    .toList();

            DecisionReportDto report = new DecisionReportDto(Instant.now(clock), summary, items);
            mapper.writeValue(outputDir.resolve("decisions.json").toFile(), report);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи decisions.json в " + outputDir, e);
        }
    }

    // 2. Создание файла summary.txt
    public void writeSummaryTxt(
            Path outputDir,
            Repository<BigInteger, PaymentDecision> repository,
            Map<Long, Client> clients,
            List<ErrorResult> clientErrors,
            List<ErrorResult> paymentErrors
    ) {
        validateClientsMap(clients);

        try {
            Files.createDirectories(outputDir);

            List<PaymentDecision> decisions = getSortedDecisions(repository);
            SummaryDTO summary = calculateStats(decisions, clientErrors, paymentErrors);
            Map<Long, List<PaymentDecision>> byClient = decisions.stream()
                    .collect(Collectors.groupingBy(d -> d.request().clientId(), TreeMap::new, Collectors.toList()));

            Path file = outputDir.resolve("summary.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("""
                        PAYMENT CLEARANCE REPORT
                        Проверено поручений: %d
                        Одобрено: %d
                        Отложено: %d
                        Отклонено: %d
                        Ошибок импорта: %d
                        Сумма одобренных платежей: %s
                        Сумма комиссий: %s
                        Статистика по клиентам:
                        """.formatted(
                        summary.totalChecked(), summary.approvedCount(), summary.scheduledCount(),
                        summary.rejectedCount(), summary.importErrorsCount(),
                        summary.totalApprovedAmount(), summary.totalCommissionAmount()
                ));

                for (var entry : byClient.entrySet()) {
                    long clientId = entry.getKey();
                    SummaryDTO clientStats = calculateStats(entry.getValue(), List.of(), List.of());
                    String name = clients.containsKey(clientId) ? clients.get(clientId).name() : "UNKNOWN";

                    writer.write("  Клиент #%d (%s): одобрено=%d, отложено=%d, отклонено=%d, сумма=%s, комиссия=%s%n"
                            .formatted(clientId, name, clientStats.approvedCount(), clientStats.scheduledCount(),
                                    clientStats.rejectedCount(), clientStats.totalApprovedAmount(), clientStats.totalCommissionAmount()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи summary.txt в " + outputDir, e);
        }
    }

    // 3. Создание файла import-errors.txt
    public void writeImportErrorsTxt(
            Path outputDir,
            List<ErrorResult> clientErrors,
            List<ErrorResult> paymentErrors
    ) {
        try {
            Files.createDirectories(outputDir);

            Path file = outputDir.resolve("import-errors.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("=== clients.csv (ошибок: " + clientErrors.size() + ") ===\n");
                for (ErrorResult err : clientErrors) {
                    writer.write("Строка %d | %s | %s%n".formatted(err.num(), err.msgError(), err.text()));
                }

                writer.newLine();

                writer.write("=== payments.csv (ошибок: " + paymentErrors.size() + ") ===\n");
                for (ErrorResult err : paymentErrors) {
                    writer.write("Строка %d | %s | %s%n".formatted(err.num(), err.msgError(), err.text()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи import-errors.txt в " + outputDir, e);
        }
    }

    private void validateClientsMap(Map<Long, Client> clients) {
        if (clients == null) {
            throw new IllegalArgumentException("Карта клиентов не может быть null.");
        }
        for (var entry : clients.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getKey() != entry.getValue().id()) {
                throw new IllegalArgumentException("Несоответствие ключа и ID клиента в карте: " + entry.getKey());
            }
        }
    }

    private List<PaymentDecision> getSortedDecisions(Repository<BigInteger, PaymentDecision> repository) {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(d -> d.request().id()))
                .toList();
    }

    private SummaryDTO calculateStats(
            List<PaymentDecision> decisions,
            List<ErrorResult> clientErrors,
            List<ErrorResult> paymentErrors
    ) {
        int approved = 0;
        int scheduled = 0;
        int rejected = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (PaymentDecision d : decisions) {
            switch (d) {
                case ApprovedPayment a -> {
                    approved++;
                    totalAmount = totalAmount.add(a.request().amount());
                    totalCommission = totalCommission.add(a.commission());
                }
                case ScheduledPayment s -> scheduled++;
                case RejectedPayment r -> rejected++;
            }
        }

        int importErrorsCount = clientErrors.size() + paymentErrors.size();

        return new SummaryDTO(
                decisions.size(), approved, scheduled, rejected,
                importErrorsCount, formatMoney(totalAmount), formatMoney(totalCommission)
        );
    }

    private DecisionDTO toDto(PaymentDecision decision, Client client) {
        PaymentRequest req = decision.request();
        Instant scheduledUtc = null;
        if (client != null) {
            List<ZoneOffset> offsets = client.zoneId().getRules().getValidOffsets(req.scheduledLocalTime());
            if (offsets.size() == 1) {
                scheduledUtc = req.scheduledLocalTime().toInstant(offsets.get(0));
            }
        }

        return switch (decision) {
            case ApprovedPayment a -> new DecisionDTO(
                    req.id(), req.clientId(), formatMoney(req.amount()),
                    "APPROVED", formatMoney(a.commission()), null, scheduledUtc
            );
            case ScheduledPayment s -> new DecisionDTO(
                    req.id(), req.clientId(), formatMoney(req.amount()),
                    "SCHEDULED", null, null, s.scheduleDate()
            );
            case RejectedPayment r -> new DecisionDTO(
                    req.id(), req.clientId(), formatMoney(req.amount()),
                    "REJECTED", null, r.msgReject().name(), scheduledUtc
            );
        };
    }

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
