package thread.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import thread.model.enums.PaymentCategory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PaymentRequestTest {

    @Test
    public void shouldValidParams(){
        Instant instant = Instant.now();
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.of(2026,2,14), LocalTime.of(10,0));
        PaymentRequest request = new PaymentRequest(BigInteger.ONE,1,new BigDecimal("1800.00"),PaymentCategory.TRANSFER,instant,dateTime);

        assertEquals(BigInteger.ONE,request.getId());
        assertEquals(1,request.clientId());
        assertEquals(new BigDecimal("1800.00"),request.amount());
        assertEquals(PaymentCategory.TRANSFER,request.category());
        assertEquals(instant,request.createdAtUtc());
        assertEquals(dateTime,request.scheduledLocalTime());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-1","0","-999"})
    public void shouldIdWithNegativeScript(BigInteger id){

        long cliId = 1;
        BigDecimal amount = new BigDecimal("100.00");
        PaymentCategory category = PaymentCategory.TRANSFER;
        Instant createAt = Instant.now();
        LocalDateTime dateTime = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class,()-> new PaymentRequest(id,cliId,amount,category,createAt,dateTime));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1,0})
    public void shouldClientIdBefore0(long id){
        BigDecimal amount = new BigDecimal("100.00");
        PaymentCategory category = PaymentCategory.TRANSFER;
        Instant createAt = Instant.now();
        LocalDateTime dateTime = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,()->new PaymentRequest(BigInteger.ONE,id,amount,category,createAt,dateTime));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0","-152.010"})
    public void shouldThrowsWhereAmountNullOrZero(BigDecimal amount){
        PaymentCategory category = PaymentCategory.TRANSFER;
        Instant createAt = Instant.now();
        LocalDateTime dateTime = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class,()-> new PaymentRequest(BigInteger.ONE,1,amount,category,createAt,dateTime));

    }

    @Test
    public void shouldThrowsRandomParam(){

        long cliId = 1;
        BigDecimal amount = new BigDecimal("100.00");
        PaymentCategory category = PaymentCategory.TRANSFER;
        Instant createAt = Instant.now();
        LocalDateTime dateTime = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class,()-> new PaymentRequest(BigInteger.ONE,cliId,amount,null,createAt,dateTime));
        assertThrows(IllegalArgumentException.class,()->new PaymentRequest(BigInteger.ONE,cliId,amount,category,null,dateTime));
        assertThrows(IllegalArgumentException.class,()->new PaymentRequest(BigInteger.ONE,cliId,amount,category,createAt,null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.123", "0.001", "55.999"})
    public void shouldThrowWhenAmountHasMoreThanTwoDecimalPlaces(BigDecimal invalidScaleAmount) {
        assertThrows(ArithmeticException.class, () ->
                new PaymentRequest(
                        BigInteger.ONE,
                        1,
                        invalidScaleAmount,
                        PaymentCategory.TRANSFER,
                        Instant.now(),
                        LocalDateTime.now()
                )
        );
    }
}
