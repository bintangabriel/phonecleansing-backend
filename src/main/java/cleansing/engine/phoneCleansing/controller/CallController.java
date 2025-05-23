package cleansing.engine.phoneCleansing.controller;

import ch.loway.oss.ari4java.AriVersion;
import cleansing.engine.phoneCleansing.dto.CallResultRequest;
import cleansing.engine.phoneCleansing.dto.CallResultResponse;
import cleansing.engine.phoneCleansing.service.CallResultService;
import cleansing.engine.phoneCleansing.service.CallService;
//import cleansing.engine.phoneCleansing.service.OutboundCallService;
import cleansing.engine.phoneCleansing.service.CallServiceAri;
import cleansing.engine.phoneCleansing.service.CallServiceAriImpl;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CallController {

    @Autowired
    private CallService callService;

    @Autowired
    private CallResultService callResultService;
    @Autowired
    private CallServiceAri callServiceAri;
//    @Autowired
//    private OutboundCallService outboundCallService;

    private final Dotenv dotenv = Dotenv.load();

    @PostMapping("/call")
    public Map<String, String> makeCall(@RequestParam String extension) {
        return callService.makeCall(extension);
    }

    @PostMapping("/call-with-ari")
    public void makeCallAri(@RequestParam String extension) {
        callServiceAri.callTrigger(extension);
    }

    @PostMapping("/call/terminate")
    public Map<String, String> terminateCall(@RequestParam String actionId) {
        return callService.terminateCall(actionId);
    }

    @PostMapping("/call-dial")
    public Map<String, String> makeDialCall(@RequestParam String extension) {
        return callService.makeCallWithDial(extension);
    }

    @PostMapping("/call-result")
    public CallResultResponse<Map<String, Object>> listCallResults(@RequestBody CallResultRequest request) {
        Map<String, Object> result = callResultService.getListCallResults(request);
        return new CallResultResponse<>(true, result);
    }
}
