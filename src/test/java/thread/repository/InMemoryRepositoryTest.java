package thread.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thread.model.Client;
import thread.model.enums.Rate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryRepositoryTest {
    private Repository<Long, Client> repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository<>();
    }

    @Test
    void shouldSaveAndFindClientById() {
        Client anna = new Client(1L, "Анна", Rate.BASIC, LocalDate.of(2023, 4, 10), ZoneId.of("Europe/Berlin"));

        boolean saved = repository.save(anna);
        Optional<Client> found = repository.findById(1L);

        assertTrue(saved);
        assertTrue(found.isPresent());
        assertEquals(anna, found.get());
    }

    @Test
    void shouldReturnEmptyWhenIdIsUnknown() {
        Optional<Client> found = repository.findById(999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldNotOverwriteOnDuplicateId() {
        Client first = new Client(1L, "Анна", Rate.BASIC, LocalDate.of(2023, 4, 10), ZoneId.of("Europe/Berlin"));
        Client duplicate = new Client(1L, "Дубликат", Rate.PREMIUM, LocalDate.of(2025, 1, 1), ZoneId.of("Europe/Moscow"));

        boolean firstSaved = repository.save(first);
        boolean secondSaved = repository.save(duplicate);

        assertTrue(firstSaved);
        assertFalse(secondSaved); // Повторный ID отклонен
        assertEquals("Анна", repository.findById(1L).orElseThrow().name()); // Старая запись сохранилась
    }

    @Test
    void shouldReturnUnmodifiableListFromFindAll() {
        Client anna = new Client(1L, "Анна", Rate.BASIC, LocalDate.of(2023, 4, 10), ZoneId.of("Europe/Berlin"));
        Client ivan = new Client(2L, "Иван", Rate.PREMIUM, LocalDate.of(2022, 1, 1), ZoneId.of("Europe/Moscow"));
        repository.save(anna);

        List<Client> all = repository.findAll();

        assertEquals(1, all.size());
        // Попытка изменить список снаружи должна упасть с UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> all.add(ivan));
    }

    @Test
    void shouldThrowWhenSavingNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

}
