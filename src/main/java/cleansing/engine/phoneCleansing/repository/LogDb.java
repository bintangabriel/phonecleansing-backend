package cleansing.engine.phoneCleansing.repository;

import cleansing.engine.phoneCleansing.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogDb extends JpaRepository<Log, String> {
    Optional<Log> findById(String id);
    Optional<Log> findByAction(String actionId);
}
