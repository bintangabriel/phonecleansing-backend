package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.dto.CallResultRequest;
import cleansing.engine.phoneCleansing.model.CallResult;
import cleansing.engine.phoneCleansing.repository.CallResultDb;
import cleansing.engine.phoneCleansing.repository.CallResultFilteredDb;
import cleansing.engine.phoneCleansing.specification.CallResultSpecification;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CallResultServiceImpl implements CallResultService{

    @Autowired
    CallResultDb callResultDb;
    @Autowired
    CallResultFilteredDb callResultFilteredDb;
    @Override
    public CallResult addCallResult(CallResult callResult) {
        return callResultDb.save(callResult);
    }
    @Override
    public boolean isAnswered(String phoneNumber) {
        Optional<CallResult> callResult = callResultDb.findByPhoneNumber(phoneNumber);
        return callResult.get().getResultCall().equals("contacted");
    }
    @Override
    public long lastUpdatedDurationToNow(String phoneNumber) {
        CallResult lastCallResult = getLatestCallResultByPhoneNumber(phoneNumber);
        if (lastCallResult == null || lastCallResult.getLastUpdated() == null){
            return 1000000000;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastCallResult.getLastUpdated(), now);
        return duration.toMinutes();
    }
    @Override
    public CallResult getLatestCallResultByPhoneNumber(String phoneNumber) {
        return callResultDb.findTopByPhoneNumberOrderByLastUpdatedDesc(phoneNumber);
    }
    @Override
    public Map<String, Object> getListCallResults(CallResultRequest callResultRequest) {
        Specification<CallResult> spec = Specification.where(CallResultSpecification.hasKeyword(callResultRequest.getKeyword()).and(CallResultSpecification.hasStatus(callResultRequest.getStatus())));

        Pageable pageable = PageRequest.of(Integer.parseInt(callResultRequest.getPage())-1, Integer.parseInt(callResultRequest.getSize()), Sort.by("lastUpdated").descending());
        Page<CallResult> page = callResultFilteredDb.findAll(spec, pageable);
        System.out.println("page: " + page);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());
        return response;
    }
}
