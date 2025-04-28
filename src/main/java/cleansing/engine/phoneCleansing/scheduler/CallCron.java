package cleansing.engine.phoneCleansing.scheduler;

import cleansing.engine.phoneCleansing.service.CallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CallCron {
    @Autowired
    private CallService callService;

    // TODO: Change to spesific time
    @Scheduled(fixedRate = 50000)
    public void callCron() {
        LocalDate localDate = LocalDate.now();
        System.out.println("Cleansing process started for: " + localDate.toString());
        String[] extension = {"101", "103"};
        for (int i = 0; i < 2; i++) {
            callService.makeCall(extension[i]);
        }
    }
}
