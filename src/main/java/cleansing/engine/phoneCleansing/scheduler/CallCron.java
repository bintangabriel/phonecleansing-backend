package cleansing.engine.phoneCleansing.scheduler;

import cleansing.engine.phoneCleansing.service.CallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class CallCron {
    @Autowired
    private CallService callService;

    // TODO: Change to spesific time
    // Comment the scheduled time in development if you dont wanna get annoyed
//    @Scheduled(fixedRate = 50000)
    public void callCron() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        LocalDate localDate = LocalDate.now();
        System.out.println("Cleansing process started for: " + localDate.toString());
        String[] extension = {"104", "103"};
        // TODO: Add to real caller numbers
        for (int i = 0; i < 2; i++) {
            int phoneIndex = i;
            executor.submit(() -> {
                callService.makeCall(extension[phoneIndex]);
            });
        }
    }

//    @Scheduled(fixedRate = 50000)
    public void callCronWithoutThread() {
        LocalDate localDate = LocalDate.now();
        System.out.println("Cleansing process started for: " + localDate.toString());
        String[] extension = {"101", "103"};
        for (int i = 0; i < 2; i++) {
            callService.makeCall(extension[i]);
        }
    }
}
