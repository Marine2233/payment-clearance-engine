package thread.repository;
import thread.model.Identifiable;
import java.util.*;

public class InMemoryRepository<K,T extends Identifiable<K>>  implements Repository<K,T> {
    private final Map<K, T> repository = new HashMap<>();

    @Override
    public boolean save(T value) {
        if (value == null || value.getId() == null){
            throw new IllegalArgumentException("Переданы невалидные данные.");
        }
        return repository.putIfAbsent(value.getId(), value) == null;
    }

    @Override
    public Optional<T> findById(K key) {
        if (key == null){
            return Optional.empty();
        }
        return Optional.ofNullable(repository.get(key));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(repository.values());
    }
}
