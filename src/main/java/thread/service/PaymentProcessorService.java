package thread.service;

import lombok.AllArgsConstructor;
import thread.exception.PaymentByIdException;
import thread.model.*;
import thread.model.enums.ReasonReject;
import thread.repository.InMemoryRepository;
import thread.repository.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class PaymentProcessorService {

    private static final Duration MIN_LEAD_TIME = Duration.ofMinutes(30);

    private final Map<Long, Client> clients;
    private final Repository<BigInteger, PaymentDecision> repository;
    private final CommissionCalculator calculator;
    private final Clock clock;

    public PaymentProcessorService(Repository<Long, Client> clientRepository) {
        this(
                clientRepository == null ? null : clientRepository.findAll().stream()
                        .collect(Collectors.toMap(Client::id, client -> client)),
                new InMemoryRepository<>(),
                new CommissionCalculator(),
                Clock.systemUTC()
        );
    }

    public PaymentProcessorService(
            Map<Long, Client> clients,
            Repository<BigInteger, PaymentDecision> repository,
            CommissionCalculator calculator,
            Clock clock
    ) {
        if (clients == null) {
            throw new IllegalArgumentException("Карта клиентов не может быть null.");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Репозиторий решений не может быть null.");
        }
        if (calculator == null) {
            throw new IllegalArgumentException("Калькулятор комиссий не может быть null.");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock не может быть null.");
        }
        this.clients = clients;
        this.repository = repository;
        this.calculator = calculator;
        this.clock = clock;
    }

    public PaymentDecision process(PaymentRequest request){

        if (request == null){
            throw new IllegalArgumentException("Поручение не может быть null");
        }

        PaymentDecision paymentDecision = repository.findById(request.getId()).orElse(null);

        if (paymentDecision != null ){
            if (request.equals(paymentDecision.request())){
                return paymentDecision;
            }else {
                throw new PaymentByIdException("Конфликт ID поручения: платеж с ID " + request.id() + " уже обработан с другими данными.");
            }
        }

        Instant now = Instant.now(clock);
        Client client = clients.get(request.clientId());
        if (client == null) {
            return saveAndReturn(new RejectedPayment(request, ReasonReject.CLIENT_NOT_FOUND, now));
        }

        LocalDateTime utc = request.scheduledLocalTime();
        ZoneId zoneId = client.zoneId();
        List<ZoneOffset> zoneIds = zoneId.getRules().getValidOffsets(utc);

        if (zoneIds.isEmpty()){
            return saveAndReturn(new RejectedPayment(request, ReasonReject.NONEXISTENT_LOCAL_TIME, now));
        }
        if (zoneIds.size() > 1){
            return saveAndReturn(new RejectedPayment(request, ReasonReject.AMBIGUOUS_LOCAL_TIME, now));
        }

        Instant scheduledPaymentAtUtc = utc.toInstant(zoneIds.getFirst());

        Duration duration = Duration.between(scheduledPaymentAtUtc,request.createdAtUtc());
        if (duration.compareTo(MIN_LEAD_TIME) > 0){
            return saveAndReturn(new RejectedPayment(request,ReasonReject.INVALID_LEAD_TIME,now));
        }

        if (scheduledPaymentAtUtc.isAfter(now)){
            return saveAndReturn(new ScheduledPayment(request,scheduledPaymentAtUtc,now));
        }

        LocalDate bankDay = utc.toLocalDate();
        BigDecimal currentLimit = currentLimit(client.id(), bankDay);
        BigDecimal totalLimit = currentLimit.add(request.amount());
        BigDecimal limitForDay = calculator.getDailyLimit(client.rate());

        if (limitForDay.compareTo(totalLimit) > 0){
            return saveAndReturn(new RejectedPayment(request,ReasonReject.DAILY_LIMIT_EXCEEDED,now));
        }

        BigDecimal precent = calculator.calculate(request,client);
        ApprovedPayment approved = new ApprovedPayment(request, precent, bankDay, now);

        return saveAndReturn(approved);

    }

    private BigDecimal currentLimit(long idCli,LocalDate bankDay){
        return repository.findAll().stream()
                .filter(paymentDecision -> paymentDecision instanceof ApprovedPayment)
                .map(paymentDecision -> (ApprovedPayment)paymentDecision)
                .filter(approvedPayment -> approvedPayment.dateBank().equals(bankDay))
                .filter(approvedPayment -> approvedPayment.request().clientId() == idCli)
                .map(approvedPayment -> approvedPayment.request().amount())
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    private PaymentDecision saveAndReturn(PaymentDecision decision) {
        repository.save(decision);
        return decision;
    }

    public Instant convertToUtc(PaymentRequest request){

        if (request == null){
            throw new IllegalArgumentException("Поручения не существует");
        }

        Client client = clients.get(request.clientId());
        if (client == null) {
            throw new IllegalArgumentException("Клиента не существует.");
        }

        List<ZoneOffset> validZone = client.zoneId().getRules().getValidOffsets(request.scheduledLocalTime());

        if (validZone.isEmpty()){
            throw new IllegalArgumentException("Несуществующее локальное время");
        }
        if (validZone.size() > 1){
            throw new IllegalArgumentException("Неопределенное локальное время.");
        }

        LocalDateTime payment = request.scheduledLocalTime();

        return ZonedDateTime.of(payment,client.zoneId()).toInstant();
    }
}
