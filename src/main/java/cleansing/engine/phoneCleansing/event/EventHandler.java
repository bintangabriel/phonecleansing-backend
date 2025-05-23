//package cleansing.engine.phoneCleansing.event;
//
//import ch.loway.oss.ari4java.generated.models.Event;
//
//import java.util.EventListener;
//
//public class EventHandler implements EventListener {
//    @Override
//    public void onEvent(Event event) {
//        System.out.println("Received event: " + event.getType());
//
//        // Handle specific events
//        if ("ChannelStateChange".equals(event.getType())) {
//            var channel = event.getApplication();
//            if (channel != null) {
//                System.out.println("Channel: " + channel.getName() + ", State: " + channel.getState());
//            }
//        }
//    }
//}
