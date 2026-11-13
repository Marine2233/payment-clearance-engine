package thread.model;

import thread.model.enums.Rate;

import java.time.LocalDate;
import java.time.ZoneId;

public record  Client (long id,
                       String name,
                       Rate rate,
                       LocalDate dataRegistry,
                       ZoneId zoneId) implements Identifiable<Long>
{
    public Client{

        if (id <= 0) {
            throw  new IllegalArgumentException("id должен быть строго положительный.");
        }
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("Имя не может быть null.");
        }
        if (rate == null) {
            throw new IllegalArgumentException("Невалидный тариф.");
        }
        if (dataRegistry == null){
            throw new IllegalArgumentException("Невалидная дата регистрации.");
        }
        if (zoneId == null){
            throw new IllegalArgumentException("Невалидный часовой пояс/локализация.");
        }

    }

    @Override
    public Long getId() {
        return id;
    }
}
