package cleansing.engine.phoneCleansing.service;

public interface CallService{
    void openConnection();

    String makeCall(String extension);
    String extractCalleeFromChannel(String channel);
}
