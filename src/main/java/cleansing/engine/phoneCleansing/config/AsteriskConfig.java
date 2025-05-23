//package cleansing.engine.phoneCleansing.config;
//
//import cleansing.engine.phoneCleansing.model.AMDStatusEvent;
//import org.asteriskjava.manager.event.UserEvent;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.asteriskjava.manager.*;
//
//import java.lang.reflect.Field;
//import java.util.Map;
//
//@Configuration
//public class AsteriskConfig {
//    @Bean
//    public boolean registerCustomEvents() {
//        try {
//            EventBuilderImpl eventBuilder = new EventBuilderImpl();
//
//            // Use reflection to access the private userEventClasses map
//            Field userEventClassesField = EventBuilderImpl.class.getDeclaredField("userEventClasses");
//            userEventClassesField.setAccessible(true);
//
//            @SuppressWarnings("unchecked")
//            Map<String, Class<? extends UserEvent>> userEventClasses =
//                    (Map<String, Class<? extends UserEvent>>) userEventClassesField.get(eventBuilder);
//
//            // Register our custom event class for AMDStatus events
//            userEventClasses.put("AMDStatus", AMDStatusEvent.class);
//
//            System.out.println("Successfully registered AMDStatus event class!");
//            return true;
//        } catch (Exception e) {
//            System.err.println("Failed to register custom event classes: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//    }
//}
