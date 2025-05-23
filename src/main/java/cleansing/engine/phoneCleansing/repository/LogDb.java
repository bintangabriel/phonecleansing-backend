package cleansing.engine.phoneCleansing.repository;

import cleansing.engine.phoneCleansing.model.CallResult;
import cleansing.engine.phoneCleansing.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogDb extends JpaRepository<Log, String> {
    Optional<Log> findById(String id);
    Log findByAction(String actionId);
    Optional<Log> findByChannel(String channel);
}
