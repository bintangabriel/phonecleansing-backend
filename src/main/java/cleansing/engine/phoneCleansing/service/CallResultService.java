package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.dto.CallResultRequest;
import cleansing.engine.phoneCleansing.model.CallResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CallResultService {
    CallResult addCallResult(CallResult callResult);

    boolean isAnswered(String phoneNumber);
    long lastUpdatedDurationToNow(String phoneNumber);
    CallResult getLatestCallResultByPhoneNumber(String phoneNumber);
    Map<String, Object> getListCallResults(CallResultRequest callResultRequest);
}
