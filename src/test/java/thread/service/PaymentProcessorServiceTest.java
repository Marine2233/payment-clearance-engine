package thread.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thread.model.Client;
import thread.model.PaymentDecision;
import thread.model.PaymentRequest;
import thread.model.enums.PaymentCategory;
import thread.model.enums.Rate;
import thread.repository.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProcessorServiceTest {

    @Mock
    private Map<Long, Client> clients;

    @Mock
    private Repository<BigInteger, PaymentDecision> repository;

    @Mock
    private CommissionCalculator calculator;

    @Mock
    private Clock clock;

    @InjectMocks
    private PaymentProcessorService service;

    private Client makeClient() {
        return new Client(
                1L,
                "Cli-1",
                Rate.PREMIUM,
                LocalDate.of(2026, 2, 19),
                ZoneId.of("Europe/Berlin")
        );
    }

    private PaymentRequest makePaymentRequest(long clId, LocalDateTime scheduledLocal) {
        return new PaymentRequest(
                BigInteger.ONE,
                clId,
                new BigDecimal("1800.00"),
                PaymentCategory.TRANSFER,
                Instant.parse("2026-11-11T10:00:00Z"),
                scheduledLocal
        );
    }

    @Test
    public void convertToUtc_WhenClientContainRepository(){

        Client client = makeClient();
        LocalDateTime localTime = LocalDateTime.of(LocalDate.of(2026, 12, 12), LocalTime.of(10, 0));
        PaymentRequest request = makePaymentRequest(client.id(),localTime);

        when(clients.get(client.id())).thenReturn(client);

        Instant res = service.convertToUtc(request);

        assertEquals(Instant.parse("2026-12-12T09:00:00Z"),res);
        verify(clients,times(1)).get(client.id());

    }

    @Test
    public void convertToUtc_WhenClientNotContainsRepository(){

        LocalDateTime localTime = LocalDateTime.of(LocalDate.of(2026, 12, 12), LocalTime.of(10, 0));
        PaymentRequest request = makePaymentRequest(1,localTime);

        when(clients.get(request.clientId())).thenReturn(null);

        assertThrows(IllegalArgumentException.class,()-> service.convertToUtc(request));

    }

    @Test
    public void convertToUtc_WhenTimeDoesNotExistInSpring_ShouldThrow() {
        Client client = makeClient();
        // 29 марта 2026 02:30 в Берлине не существует (перевод часов на летнее время)
        PaymentRequest request = makePaymentRequest(client.id(), LocalDateTime.of(2026, 3, 29, 2, 30));

        when(clients.get(client.id())).thenReturn(client);

        assertThrows(IllegalArgumentException.class, () -> service.convertToUtc(request));
    }

    @Test
    public void convertToUtc_WhenTimeIsAmbiguousInAutumn_ShouldThrow() {

        Client client = makeClient();
        // 25 октября 2026 02:30 в Берлине наступает дважды (перевод часов на зимнее время)
        PaymentRequest request = makePaymentRequest(client.id(), LocalDateTime.of(2026, 10, 25, 2, 30));

        when(clients.get(client.id())).thenReturn(client);

        assertThrows(IllegalArgumentException.class, () -> service.convertToUtc(request));
    }

    @Test
    public void shouldThrowsWhenParamNull(){

        assertThrows(IllegalArgumentException.class,()-> service.convertToUtc(null));
    }


}
