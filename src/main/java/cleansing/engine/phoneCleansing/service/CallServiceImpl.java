package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.model.Log;
import cleansing.engine.phoneCleansing.repository.LogDb;
import cleansing.engine.phoneCleansing.util.Constant;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.HangupAction;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.response.ManagerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Service
public class CallServiceImpl implements CallService, ManagerEventListener {
    @Autowired
    LogDb logDb;
    private ManagerConnection managerConnection;
    private final Dotenv dotenv = Dotenv.load();

    @Override
    public void openConnection() {
        try {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(
                    dotenv.get("ASTERISK_SERVER"),
                    dotenv.get("MANAGER_USERNAME"),
                    dotenv.get("MANAGER_PASSWORD")
            );
            managerConnection = factory.createManagerConnection();
            managerConnection.login();
            managerConnection.addEventListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public Map<String, String> makeCall(String extension) {
        openConnection();
        Map<String, String> data = new HashMap<>();
        String actionId = Constant.ACTION_ID;
        String callerId = Constant.CALLER_ID;
        String uuid = callerId.substring(5);
        try {
            OriginateAction originateAction = new OriginateAction();
            originateAction.setActionId(actionId);
            originateAction.setCallerId(callerId + " <" + uuid + ">");

            // Direct call to extension
//            originateAction.setChannel(Constant.PREFIX_PROTOCOL + extension);
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
            System.out.println("Caller id: " + originateAction.getCallerId());
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
    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof NewStateEvent e) {
            System.out.println("📡 Channel state: " + e.getChannelStateDesc());

            switch (e.getChannelStateDesc()) {
                case "Ringing" -> {
                    System.out.println(e.getChannel());
                    String callee = extractCalleeFromChannel(e.getChannel());
                    System.out.println("📞 Ringing to: " + callee);
                }
                case "Up" -> {
                    String callee = extractCalleeFromChannel(e.getChannel());
                    System.out.println("✅ Answered by: " + callee);
                }
            }
        } else if (event instanceof HangupEvent e) {
            System.out.println("🔴 Hangup with cause: " + e.getCauseTxt());
        } else if (event instanceof DialEvent e) {
            System.out.println("🔄 Dialing from " + e.getCallerIdNum() +  " to " + e.getDestination());
        } else if (event instanceof VarSetEvent e) {
            String channel = e.getChannel();
            String actionId = e.getValue();
            if (channel != null && actionId.startsWith("id-call-")) {
                Log log = new Log();
                log.setAction(e.getValue());
                log.setChannel(e.getChannel());
                logDb.save(log);
            }
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
        Optional<Log> log = logDb.findByAction(actionId);
        String channel = log.get().getChannel();
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
}
