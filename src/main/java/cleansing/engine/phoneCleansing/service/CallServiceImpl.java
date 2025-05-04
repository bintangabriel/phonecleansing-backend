package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.util.Constant;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.response.ManagerResponse;
import org.springframework.stereotype.Service;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class CallServiceImpl implements CallService, ManagerEventListener {
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
    public String makeCall(String extension) {
        openConnection();
        try {
            String actionId = Constant.ACTION_ID;
            OriginateAction originateAction = new OriginateAction();
            originateAction.setActionId(actionId);
            originateAction.setCallerId(Constant.CALLER_ID);

            // Direct call to extension
//            originateAction.setChannel(Constant.PREFIX_PROTOCOL + extension);
            originateAction.setChannel(Constant.PREFIX_PROTOCOL + extension + Constant.SIP_PROVIDER);
            originateAction.setApplication("Playback");
            originateAction.setData(Constant.AUDIO_FILE_NAME);

            // Keep it async
            originateAction.setAsync(true);

            ManagerResponse response = managerConnection.sendAction(originateAction, 30000);
            return response.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
            return "Call failed: " + e.getMessage();
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
        }

    }

    @Override
    public String extractCalleeFromChannel(String channel) {
        if (channel != null && channel.contains("/")) {
            return channel.split("/")[1].split("-")[0];
        }
        return "Unknown";
    }
}
