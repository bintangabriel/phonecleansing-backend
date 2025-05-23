package cleansing.engine.phoneCleansing.repository;

import cleansing.engine.phoneCleansing.model.CallResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CallResultDb extends JpaRepository<CallResult, String> {
    Optional<CallResult> findById(String id);
    Optional<CallResult> findByPhoneNumber(String phoneNumber);
    CallResult findTopByPhoneNumberOrderByLastUpdatedDesc(String phoneNumber);
}
