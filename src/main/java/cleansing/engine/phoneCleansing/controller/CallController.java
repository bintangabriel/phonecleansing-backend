package cleansing.engine.phoneCleansing.controller;

import ch.loway.oss.ari4java.AriVersion;
import cleansing.engine.phoneCleansing.service.CallService;
import cleansing.engine.phoneCleansing.service.OutboundCallService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CallController {

    @Autowired
    private CallService callService;

    @Autowired
    private OutboundCallService outboundCallService;

    private final Dotenv dotenv = Dotenv.load();

    @PostMapping("/call")
    public String makeCall(@RequestParam String extension) {
        return callService.makeCall(extension);
    }

    @PostMapping("/call-with-ari")
    public void makeCallAri() {
        String serverIp = dotenv.get("ASTERISK_SERVER");
        String manageUsername = dotenv.get("MANAGER_USERNAME");
        String managerPassword = dotenv.get("MANAGER_PASSWORD");
        String urlServerIp = "http://" + serverIp + ":8088";
        outboundCallService.start(urlServerIp, manageUsername, managerPassword, AriVersion.ARI_8_0_0);
    }
}
