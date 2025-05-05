package cleansing.engine.phoneCleansing.service;

import java.util.Map;

public interface CallService{
    void openConnection();

    Map<String, String> makeCall(String extension);
    String extractCalleeFromChannel(String channel);
    Map<String, String> terminateCall(String extension);
}
