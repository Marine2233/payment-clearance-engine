package thread.repository;

import java.util.List;
import java.util.Optional;

public interface Repository <K,T>{
    boolean save(T value);
    Optional<T> findById(K key);
    List<T> findAll();
}
