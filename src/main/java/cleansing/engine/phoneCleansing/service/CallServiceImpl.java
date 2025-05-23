package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.model.AMDStatusEvent;
import cleansing.engine.phoneCleansing.model.Log;
import cleansing.engine.phoneCleansing.repository.LogDb;
import cleansing.engine.phoneCleansing.util.Constant;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.HangupAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CallServiceImpl implements CallService {
    @Autowired
    LogDb logDb;
    @Autowired
    LogService logService;
    @Autowired
    LogSip logSip;
    private ManagerConnection managerConnection;
    private final Dotenv dotenv = Dotenv.load();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public void openConnection() {
        try {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(
                    dotenv.get("ASTERISK_SERVER"),
//                    Integer.parseInt(dotenv.get("ASTERISK_PORT")),
                    dotenv.get("MANAGER_USERNAME"),
                    dotenv.get("MANAGER_PASSWORD")
            );
            managerConnection = factory.createManagerConnection();
            managerConnection.login();

            while (managerConnection.getState() != ManagerConnectionState.CONNECTED) {
                Thread.sleep(100);
            }
            System.out.println("manager connection state: " + managerConnection.getState());
            managerConnection.addEventListener(logSip);
            managerConnection.registerUserEventClass(AMDStatusEvent.class);
        } catch (Exception e) {
            System.out.println("gagal connect ");
            e.printStackTrace();
        }
    }
    @Override
    public Map<String, String> makeCall(String extension) {
        openConnection();
        Map<String, String> data = new HashMap<>();
        String actionId = Constant.ACTION_ID + "-" + extension;
        String callerId = Constant.CALLER_ID;
        String uuid = callerId.substring(5);
        try {
            OriginateAction originateAction = new OriginateAction();
            originateAction.setActionId(actionId);
            originateAction.setCallerId(callerId + " <" + uuid + ">");

            // Direct call to extension
            originateAction.setChannel(Constant.PREFIX_PROTOCOL + extension + Constant.SIP_PROVIDER);
            originateAction.setApplication("Playback");
            originateAction.setData(Constant.AUDIO_FILE_NAME);

            Map<String, String> vars = new HashMap<>();
            vars.put("X-ActionId", actionId);
            originateAction.setVariables(vars);

            // Keep it async
            originateAction.setAsync(true);

            ManagerResponse response = managerConnection.sendAction(originateAction, 30000);
            data.put("status", response.getResponse());
            data.put("actionId", actionId);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            data.put("status", "Failed");
            data.put("actionId", actionId);
            data.put("data", e.getMessage());
            return data;
        }
    }
    @Override
    public String extractCalleeFromChannel(String channel) {
        if (channel != null && channel.contains("/")) {
            return channel.split("/")[1].split("-")[0];
        }
        return "Unknown";
    }
    @Override
    public Map<String, String> terminateCall(String actionId) {
        Map<String, String> data = new HashMap<>();
        Log log = logDb.findByAction(actionId);
        String channel = log.getChannel();
        if (channel != null) {
            try {
                HangupAction hangupAction = new HangupAction(channel);
                ManagerResponse response = managerConnection.sendAction(hangupAction);
                data.put("status", response.getResponse());
            } catch (Exception e) {
                e.printStackTrace();
                data.put("status", "Failed");
                data.put("data", e.getMessage());
            }
        } else {
            data.put("status", "Failed");
            data.put("data", "Call with specified action ID not found");
        }
        return data;
    }

    @Override
    public Map<String, String> makeCallWithDial(String extension) {
        openConnection();
        Map<String, String> data = new HashMap<>();
        String actionId = Constant.ACTION_ID + "-" + extension;
        try {
            // Change this to desired trunk
            OriginateAction originateAction = originateActionMaker("quiros", extension);
            Map<String, String> vars = new HashMap<>();
            vars.put("X-ActionId", actionId);
            vars.put("DEST", "0" + extension);
            originateAction.setVariables(vars);
            ManagerResponse response = managerConnection.sendAction(originateAction, 30000);
            data.put("status", response.getResponse());
            data.put("message", response.getMessage());
            data.put("actionId", actionId);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            data.put("status", "Failed");
            data.put("actionId", actionId);
            data.put("data", e.getMessage());
            return data;
        }
    }

    @Override
    public OriginateAction originateActionMaker(String trunk, String extension) {
        String actionId = Constant.ACTION_ID + "-" + extension;
        String callerId = Constant.CALLER_ID;
        OriginateAction originateAction = new OriginateAction();
        originateAction.setActionId(actionId);
        originateAction.setCallerId(callerId + " <0" + extension + ">");
        originateAction.setPriority(1);
        originateAction.setAsync(true);
        if (trunk.equalsIgnoreCase("twilio")) {
            originateAction.setChannel("Local/+62" +  extension + "@call-monitor");
            originateAction.setContext("call-monitor");
            originateAction.setExten("+62" + extension);
            originateAction.setPriority(1);
            originateAction.setAsync(true);
        } else if (trunk.equalsIgnoreCase("dalnet")) {
//            originateAction.setChannel("Local/0" +  extension + "@call-monitor-dalnet");
//            originateAction.setContext("call-monitor-dalnet");
//            originateAction.setExten("0" + extension);
//            originateAction.setChannel("Local/start@call-monitor-quiros");
            originateAction.setChannel("Local/0" + extension + "@call-monitor-quiros");
            originateAction.setContext("call-monitor-quiros");
//            originateAction.setExten("start");
            originateAction.setExten("0" + extension);
        } else if (trunk.equalsIgnoreCase("quiros")) {
            originateAction.setChannel("Local/" + "62" + extension + "@call-monitor-quiros");
            originateAction.setContext("call-monitor-quires");
            originateAction.setExten("62" + extension);
        } else if (trunk.equalsIgnoreCase("quirosIntelix")) {
            originateAction.setChannel("Local/" + "0" + extension + "@route-oc");
            originateAction.setContext("route-oc");
            originateAction.setExten("0" + extension);
        }
        return  originateAction;
    }
}
