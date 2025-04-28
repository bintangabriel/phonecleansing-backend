package cleansing.engine.phoneCleansing.service;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriVersion;
import ch.loway.oss.ari4java.generated.AriWSHelper;
import ch.loway.oss.ari4java.generated.models.AsteriskInfo;
import ch.loway.oss.ari4java.generated.models.Message;
import ch.loway.oss.ari4java.generated.models.PlaybackFinished;
import ch.loway.oss.ari4java.generated.models.StasisStart;
import ch.loway.oss.ari4java.generated.models.ChannelStateChange;
import ch.loway.oss.ari4java.tools.ARIException;
import ch.loway.oss.ari4java.tools.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
@Service
public class OutboundCallService {

    private static final String ARI_APP = "outbound-app";
    private static final String ORIGIN_EXTENSION = "101";
    private static final String DESTINATION_EXTENSION = "103";
    private static final String AUDIO_FILE = "sound:custom-message"; // Replace with your custom audio file

    private ARI ari;
    private final Logger logger = LoggerFactory.getLogger(OutboundCallService.class);
    private String channelId;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("** Expecting at least 3 arguments:\n  url user pass [ariversion]");
            System.exit(1);
        }
        AriVersion ver = AriVersion.IM_FEELING_LUCKY;
        if (args.length == 4) {
            ver = AriVersion.fromVersionString(args[3]);
        }
        new OutboundCallService().start(args[0], args[1], args[2], ver);
    }

    public void start(String url, String user, String pass, AriVersion ver) {
        logger.info("STARTING OUTBOUND CALL SERVICE");
        boolean connected = connect(url, user, pass, ver);
        if (connected) {
            try {
                initiateCall();
            } catch (Throwable t) {
                logger.error("Error: {}", t.getMessage(), t);
            } finally {
                logger.info("ARI cleanup");
                ari.cleanup();
            }
        }
        logger.info("SERVICE TERMINATED");
    }

    private boolean connect(String url, String user, String pass, AriVersion ver) {
        try {
            ari = ARI.build(url, ARI_APP, user, pass, ver);
            logger.info("ARI Version: {}", ari.getVersion().version());
            AsteriskInfo info = ari.asterisk().getInfo().execute();
            logger.info("AsteriskInfo up since {}", info.getStatus().getStartup_time());
            return true;
        } catch (Throwable t) {
            logger.error("Error connecting to ARI: {}", t.getMessage(), t);
        }
        return false;
    }

    private void initiateCall() throws InterruptedException, ARIException {
        final ExecutorService threadPool = Executors.newFixedThreadPool(10);

        // Set up event handling
        ari.eventsCallback(new AriWSHelper() {
            @Override
            public void onSuccess(Message message) {
                threadPool.execute(() -> super.onSuccess(message));
            }

            @Override
            public void onFailure(RestException e) {
                logger.error("Error: {}", e.getMessage(), e);
                threadPool.shutdown();
            }

            @Override
            protected void onStasisStart(StasisStart message) {
                handleStasisStart(message);
            }

            @Override
            protected void onChannelStateChange(ChannelStateChange message) {
                handleChannelStateChange(message);
            }

            @Override
            protected void onPlaybackFinished(PlaybackFinished message) {
                handlePlaybackFinished(message);
            }
        });

        // Originate the call directly from 101 to 102
        try {
            logger.info("Initiating call from {} to {}", ORIGIN_EXTENSION, DESTINATION_EXTENSION);
            String endpoint = "PJSIP/" + DESTINATION_EXTENSION;
            channelId = ari.channels().originate(endpoint)
                    .setApp(ARI_APP)
                    .setAppArgs("outbound-call")
                    .setCallerId(ORIGIN_EXTENSION)
                    .execute().getId();

            logger.info("Call originated with channel ID: {}", channelId);
        } catch (Throwable e) {
            logger.error("Error originating call: {}", e.getMessage(), e);
            threadPool.shutdown();
        }

        // Wait for events to process
        if (!threadPool.awaitTermination(5, TimeUnit.MINUTES)) {
            logger.warn("Timeout waiting for call to complete");
        }

        ari.cleanup();
        System.exit(0);
    }

    private void handleStasisStart(StasisStart start) {
        logger.info("Stasis Start for Channel: {}", start.getChannel().getId());
        // When the call enters the Stasis application, we continue to monitor for state changes
    }

    private void handleChannelStateChange(ChannelStateChange stateChange) {
        logger.info("Channel {} state changed to {}", stateChange.getChannel().getId(), stateChange.getChannel().getState());

        // Check if the called party (102) has answered
        if (stateChange.getChannel().getState().equals("Up")) {
            logger.info("Call has been answered! Playing audio after delay...");

            // Wait 1 second before playing audio
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    playAudioMessage(stateChange.getChannel().getId());
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting to play audio: {}", e.getMessage());
                } catch (ARIException e) {
                    logger.error("Error playing audio: {}", e.getMessage(), e);
                }
            }).start();
        }
    }

    private void playAudioMessage(String channelId) throws ARIException {
        logger.info("Playing audio message on channel {}", channelId);
        ari.channels().play(channelId, AUDIO_FILE).execute();
    }

    private void handlePlaybackFinished(PlaybackFinished playback) {
        logger.info("PlaybackFinished - {}", playback.getPlayback().getTarget_uri());

        // After playback finishes, wait 2 seconds and then hang up
        if (playback.getPlayback().getTarget_uri().contains("channel:")) {
            try {
                String chanId = playback.getPlayback().getTarget_uri().split(":")[1];
                logger.info("Scheduling hangup for channel: {}", chanId);

                // Wait 2 seconds before hanging up
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        logger.info("Hanging up channel: {}", chanId);
                        ari.channels().hangup(chanId).execute();
                    } catch (InterruptedException e) {
                        logger.error("Interrupted while waiting to hang up: {}", e.getMessage());
                    } catch (ARIException e) {
                        logger.error("Error hanging up: {}", e.getMessage(), e);
                    }
                }).start();
            } catch (Throwable e) {
                logger.error("Error processing playback finished: {}", e.getMessage(), e);
            }
        } else {
            logger.error("Cannot handle URI - {}", playback.getPlayback().getTarget_uri());
        }
    }
}