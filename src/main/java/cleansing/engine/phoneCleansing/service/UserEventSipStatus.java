package cleansing.engine.phoneCleansing.service;
import org.asteriskjava.manager.event.UserEvent;

public class UserEventSipStatus extends UserEvent {
    private String type;
    private String sipcode;
    private String dialstatus;
    private String cause;

    public UserEventSipStatus(Object source) {
        super(source);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSipcode() {
        return sipcode;
    }

    public void setSipcode(String sipcode) {
        this.sipcode = sipcode;
    }

    public String getDialstatus() {
        return dialstatus;
    }

    public void setDialstatus(String dialstatus) {
        this.dialstatus = dialstatus;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
