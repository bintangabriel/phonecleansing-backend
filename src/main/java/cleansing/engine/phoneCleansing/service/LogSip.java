package cleansing.engine.phoneCleansing.service;

import cleansing.engine.phoneCleansing.model.AMDStatusEvent;
import cleansing.engine.phoneCleansing.model.CallResult;
import cleansing.engine.phoneCleansing.model.Log;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.event.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Custom event listener specifically designed to capture SIP 183 Session Progress events
 * that might not be properly exposed through the standard NewStateEvent
 */
@Service
public class LogSip implements ManagerEventListener {
    @Autowired
    LogService logService;
    @Autowired
    CallResultService callResultService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    // Usage: Lock one phone number result for 1 call, prevent race condition
    private static final Set<String> processingNumbers = ConcurrentHashMap.newKeySet();
    private static final Set<String> processingNumbersUncontacted = ConcurrentHashMap.newKeySet();
    @Override
    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof HangupEvent e) {
            handleHangup(e);
        } else if (event instanceof AMDStatusEvent e) {
            handleUserAMDStatusEvent(e);
        } else if (event instanceof UserEvent e) {
            handleUserEvent(e);
        } else if (event instanceof OriginateResponseEvent e) {
            handleOriginateResponseEvent(e);
        } else if (event instanceof NewStateEvent e){
            handleNewStateEvent(e);
        } else if (event instanceof VarSetEvent e) {
            handleVarSetEvent(e);
        } else if (event instanceof DialEvent e) {
            handleDialEvent(e);
        }
    }

    private void handleHangup(HangupEvent event) {
        String uniqueId = event.getUniqueId();
        int cause = event.getCause();
        String causeText = event.getCauseTxt();
//        System.out.println("Unique id: " + uniqueId + " | cause: " + cause + " | causeText: " + causeText);
    }

    private void handleUserEvent(UserEvent event) {
       System.out.println("User event : " + event + " | " + event.getClass() + " | " + event.getChannelStateDesc());
    }

    private void handleUserAMDStatusEvent(AMDStatusEvent event) {
        String phoneNumber = event.getCallerIdNum();
        System.out.println("User event AMD status for " + phoneNumber + ": " + event.getStatus());
    }

    private void handleOriginateResponseEvent(OriginateResponseEvent event) {
        String response = event.getResponse();
        Integer reason = event.getReason();
        System.out.println("Originate Response: " + response + " | Reason: " + reason);
        String phoneNumber = event.getCallerIdNum();
        executorService.submit(() -> {
            try {
                if (response.equalsIgnoreCase("failure") && processingNumbersUncontacted.add(phoneNumber)) {
//                    boolean isAnswered = callResultService.isAnswered(phoneNumber);
                    CallResult callResult = new CallResult();
                    callResult.setPhoneNumber(phoneNumber);
                    switch (reason) {
                        case 0 -> {
                            System.out.println("save unconnected for: " + phoneNumber);
                            callResult.setResultCall("unconnected");
                        }
                        case  1 | 2 | 3 -> {
                            System.out.println("save uncontacted for: " + phoneNumber);
                            callResult.setResultCall("uncontacted");
                        }
                    }
                    callResultService.addCallResult(callResult);
                }
            } catch (Exception error) {
                error.printStackTrace();
            } finally {
                processingNumbersUncontacted.remove(phoneNumber);
            }
        });
    }

    private void handleNewStateEvent(NewStateEvent e) {
        String channel = e.getChannel();
        switch (e.getChannelStateDesc().toLowerCase()) {
            case "ringing" -> {
                System.out.println("Channel: " + channel + " is currently ringing...");
            }
            case "progress" -> {
                System.out.println("Channel: " + channel + " is currently SIP Progress (183)");
            }
            case "up" -> {
                System.out.println("Channel: " + channel + " is currently answered");
                String phoneNumber = e.getCallerIdNum();
                if (!phoneNumber.equalsIgnoreCase("start")){
                    if (!processingNumbers.add(phoneNumber)) {
                        System.out.println("Skipping duplicate up event for: " + phoneNumber);
                        return;
                    }
                    executorService.submit(() -> {
                        try {
                            long lastUpdatedMinutes = callResultService.lastUpdatedDurationToNow(phoneNumber);
                            System.out.println("last update for number: " + phoneNumber + " is: " + lastUpdatedMinutes);
                            if (lastUpdatedMinutes > 1) {
                                CallResult result = new CallResult();
                                result.setPhoneNumber(phoneNumber);
                                result.setResultCall("contacted");
                                System.out.println("save contacted for: " + phoneNumber);
                                callResultService.addCallResult(result);
                            }
                        } catch (Exception error) {
                            error.printStackTrace();
                        } finally {
                            processingNumbers.remove(phoneNumber);
                        }
                    });
                }
            }
        }
    }

    private void handleDialEvent(DialEvent e){
        String channel = e.getChannel();
        System.out.println("Channel: " + channel + " is currently dialing...");
    }

    private void handleVarSetEvent(VarSetEvent e) {
        try {
            String channel = e.getChannel();
            String actionId = e.getValue();
            if (channel != null && actionId.startsWith("id-call-")){
                Log log = new Log();
                log.setAction(actionId);
                log.setChannel(channel);
                Log logCompared = logService.findByActionId(actionId);
                boolean isSame = (logCompared == null);
                System.out.println("Log compared: " + logCompared + " | is null: " + isSame);
                if (isSame){
                    System.out.println("saved");
                    logService.addLog(log);
                }
            }
        } catch (Exception error) {
            System.out.println("Error when try to store log varset");
            error.printStackTrace();
        }
    }
}