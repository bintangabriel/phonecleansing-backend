package cleansing.engine.phoneCleansing.service;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.models.Channel;
import ch.loway.oss.ari4java.tools.RestException;
import ch.loway.oss.ari4java.tools.http.NettyHttpClient;
import cleansing.engine.phoneCleansing.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;


@Service
public class CallServiceAriImpl implements CallServiceAri{

    private static final Logger logger = LoggerFactory.getLogger(CallServiceAriImpl.class);
    private ARI ari;
    private NettyHttpClient httpClient;

    @Override
    public void openConnectionAri() {
        String host = "http://" + Constant.ASTERISK_SERVER + ":8088";
        String user = Constant.ARI_USERNAME;
        String password = Constant.ARI_PASSWORD;
        try {
            ari = ARI.build(host, "cleansing-app", user, password, AriVersion.IM_FEELING_LUCKY);
//            httpClient = new NettyHttpClient();
//            System.out.println("ari: " + ari);
//            ari.setHttpClient(httpClient);
//            ari.applications().subscribe("cleansing-app", "channel:,bridge:,playback:").execute();
//            System.out.println("Subscribed to application events");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeConnection() {
        if (httpClient != null){
            httpClient.destroy();
            logger.info("ARI Connection Closed");
        }
    }

    @Override
    public CompletableFuture<Channel> makeCallWithAri(String extension, String context, String applicationName) {
        try {
            openConnectionAri();
            System.out.println("ari after open connection: " + ari);
            // Create an outbound call channel
            Channel channel = ari.channels().originate(
                                    "PJSIP/DALNET_TEST")
                    .setContext(context)
                    .setPriority(1)
                    .setExtension("0" + extension)
                    .setApp(applicationName)
                    .execute();

            logger.info("Call initiated to {} with channel ID: {}", extension, channel.getId());
            return CompletableFuture.completedFuture(channel);
        } catch (RestException e) {
            logger.error("Failed to make call to " + extension, e);
            throw new RuntimeException("Failed to make call", e);
        }
    }

    @Override
    public CompletableFuture<Void> hangupCall(String channelId) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                ari.channels().hangup(channelId).execute();
            } catch (Exception e) {
                logger.error("Failed to initiate hangup for channel " + channelId, e);
                throw new CompletionException(e);
            }
        });
        logger.info("Hangup initiated for channel {}", channelId);
        return future;
    }

    @Override
    public void callTrigger(String extension) {
        CompletableFuture<Channel> channelFuture = makeCallWithAri(extension, "call-monitor-dalnet", "ari-cleansing");

        channelFuture.thenAccept(channel -> {
            String channelId = channel.getId();
            logger.info("Call completed asynchronously with channel ID: {}", channelId);

            // Now do something with the channel
//            hangupCall(channelId);
        }).exceptionally(ex -> {
            logger.error("Error in async call handling", ex);
            return null;
        });
    }
}
