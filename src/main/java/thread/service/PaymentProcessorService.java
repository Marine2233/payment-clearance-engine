package thread.service;

import lombok.AllArgsConstructor;
import thread.model.Client;
import thread.model.PaymentRequest;
import thread.repository.InMemoryRepository;
import thread.repository.Repository;

import java.time.*;
import java.util.List;

@AllArgsConstructor
public class PaymentProcessorService {

    private final Repository<Long, Client> repository ;

    public Instant convertToUtc(PaymentRequest request){

        if (request == null){
            throw new IllegalArgumentException("Поручения не существует");
        }

        Client client = repository.findById(request.clientId()).orElseThrow(()-> new IllegalArgumentException("Клиента не существует."));

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
