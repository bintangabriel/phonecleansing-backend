package cleansing.engine.phoneCleansing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.asteriskjava.manager.event.UserEvent;

@Getter
@Setter
public class AMDStatusEvent extends UserEvent {
    static final String EVENT_TYPE = "AMDStatus";
    private String status;

    public AMDStatusEvent(Object source) {
        super(source);
    }

//    @Override
//    public String getEventName() {
//        return EVENT_TYPE;
//    }

    @Override
    public String toString() {
        return "AMDStatusEvent [status=" + status + ", " + super.toString() + "]";
    }
}
