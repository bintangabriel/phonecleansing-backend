package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.model.Log;
import cleansing.engine.phoneCleansing.repository.LogDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LogServiceImpl implements LogService{
    @Autowired
    LogDb logDb;

    @Override
    public Log addLog(Log log) {
        Log isLogExist = logDb.findByAction(log.getAction());
        if (isLogExist != null) {
            return logDb.save(log);
        }
        return null;
    }
    @Override
    public String getPhoneNumberFromChannel(String channel) {
        Optional<Log> log = logDb.findByChannel(channel);
        String actionId = log.get().getAction();
        String phoneNumber = actionId.substring(21);
        return phoneNumber;
    }
    @Override
    public boolean isLogExist(Log log) {
        Log isExist = logDb.findByAction(log.getAction());
        if (isExist != null) {
            return true;
        }
        return false;
    }

    @Override
    public Log findByActionId(String actionId) {
        return logDb.findByAction(actionId);
    }
}
