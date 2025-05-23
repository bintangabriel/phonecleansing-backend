package cleansing.engine.phoneCleansing.service;

import ch.loway.oss.ari4java.generated.models.Channel;

import java.util.concurrent.CompletableFuture;

public interface CallServiceAri {
    void openConnectionAri();
    void closeConnection();
    CompletableFuture<Channel> makeCallWithAri(String extension, String context, String applicationName);
    CompletableFuture<Void> hangupCall(String channelId);
    void callTrigger(String extension);

}
