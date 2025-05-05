package cleansing.engine.phoneCleansing.controller;

import ch.loway.oss.ari4java.AriVersion;
import cleansing.engine.phoneCleansing.service.CallService;
import cleansing.engine.phoneCleansing.service.OutboundCallService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CallController {

    @Autowired
    private CallService callService;

    @Autowired
    private OutboundCallService outboundCallService;

    private final Dotenv dotenv = Dotenv.load();

    @PostMapping("/call")
    public Map<String, String> makeCall(@RequestParam String extension) {
        return callService.makeCall(extension);
    }

    @PostMapping("/call-witsh-ari")
    public void makeCallAri() {
        String serverIp = dotenv.get("ASTERISK_SERVER");
        String manageUsername = dotenv.get("MANAGER_USERNAME");
        String managerPassword = dotenv.get("MANAGER_PASSWORD");
        String urlServerIp = "http://" + serverIp + ":8088";
        outboundCallService.start(urlServerIp, manageUsername, managerPassword, AriVersion.ARI_8_0_0);
    }

    @PostMapping("/call/terminate")
    public Map<String, String> terminateCall(@RequestParam String actionId) {
        return callService.terminateCall(actionId);
    }
}
