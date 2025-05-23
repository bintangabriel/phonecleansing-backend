package cleansing.engine.phoneCleansing.service;

import org.asteriskjava.manager.action.OriginateAction;

import java.util.Map;

public interface CallService{
    void openConnection();

    Map<String, String> makeCall(String extension);
    String extractCalleeFromChannel(String channel);
    Map<String, String> terminateCall(String extension);
    Map<String, String> makeCallWithDial(String extension);
    OriginateAction originateActionMaker(String trunk, String extension);
}
