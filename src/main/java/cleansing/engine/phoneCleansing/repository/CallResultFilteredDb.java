package cleansing.engine.phoneCleansing.repository;

import cleansing.engine.phoneCleansing.model.CallResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CallResultFilteredDb extends JpaRepository<CallResult, Integer>, JpaSpecificationExecutor<CallResult> {
}
