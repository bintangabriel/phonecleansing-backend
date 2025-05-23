package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.model.Log;

public interface LogService {
    Log addLog(Log log);
    String getPhoneNumberFromChannel(String channel);
    boolean isLogExist(Log log);
    Log findByActionId(String actionId);
}
