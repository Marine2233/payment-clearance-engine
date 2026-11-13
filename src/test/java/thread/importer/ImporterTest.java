package thread.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thread.model.Client;
import thread.model.PaymentRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

public class ImporterTest {

    private final Importer importer= new Importer();

    @Test
    void shouldImportValidPaymentsCsv(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("payments.csv");
        List<String> lines = List.of(
                "paymentId;clientId;amount;category;createdAtUtc;scheduledLocalTime",
                "1001;1;1200.00;TRANSFER;2026-11-11T10:00:00Z;2026-11-12T12:00:00",
                "1002;2;500.50;SERVICE;2026-11-11T09:00:00Z;2026-11-12T13:00:00"
        );
        Files.write(file, lines, StandardCharsets.UTF_8);

        ImportResult<PaymentRequest> result = importer.readCsvPayment(file);

        assertEquals(2, result.items().size());
        assertTrue(result.errors().isEmpty());
        assertEquals(new BigDecimal("1200.00"), result.items().get(0).amount());
    }

    @Test
    void shouldContinueImportAfterInvalidLineAndRecordError(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("payments_with_errors.csv");
        List<String> lines = List.of(
                "paymentId;clientId;amount;category;createdAtUtc;scheduledLocalTime",        // Строка 1 (заголовок)
                "1001;1;1200.00;TRANSFER;2026-11-11T10:00:00Z;2026-11-12T12:00:00",         // Строка 2 (OK)
                "1006;1;-10.00;TRANSFER;2026-11-11T10:00:00Z;2026-11-12T12:00:00",          // Строка 3 (Ошибка: сумма < 0)
                "",                                                                         // Строка 4 (пустая - пропуск)
                "1008;1;100.00;UTILITY;2026-10-24T08:00:00Z;2026-10-25T02:30:00"            // Строка 5 (OK на этапе парсинга)
        );
        Files.write(file, lines, StandardCharsets.UTF_8);

        ImportResult<PaymentRequest> result = importer.readCsvPayment(file);

        // Приняты строки 2 и 5, работа не остановилась после 3-й строки
        assertEquals(2, result.items().size());
        assertEquals(1, result.errors().size());

        // Проверяем детали ошибки
        ErrorResult error = result.errors().get(0);
        assertEquals(3, error.num());
        assertTrue(error.text().contains("-10.00"));
        assertNotNull(error.msgError());
    }

    @Test
    void shouldRejectDuplicateClientIdAndKeepFirstValid(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("clients.csv");
        List<String> lines = List.of(
                "clientId;name;tariff;zoneId;registeredOn",
                "1;Анна;INVALID_RATE;Europe/Berlin;2023-04-10",   // Строка 2: битая, не должна занять ID 1
                "1;Анна;BASIC;Europe/Berlin;2023-04-10",          // Строка 3: первая валидная -> принимается
                "1;Анна Дубль;PREMIUM;Europe/Moscow;2024-01-01"   // Строка 4: дубликат -> отклоняется
        );
        Files.write(file, lines, StandardCharsets.UTF_8);

        ImportResult<Client> result = importer.readCsvClient(file);

        assertEquals(1, result.items().size());
        assertEquals("Анна", result.items().get(0).name());
        assertEquals(2, result.errors().size());
        assertEquals(2, result.errors().get(0).num());
        assertEquals(4, result.errors().get(1).num());
    }

}
