//package cleansing.engine.phoneCleansing.service;
//
//import ch.loway.oss.ari4java.ARI;
//import ch.loway.oss.ari4java.AriVersion;
//import ch.loway.oss.ari4java.generated.models.*;
//import ch.loway.oss.ari4java.tools.RestException;
//import ch.loway.oss.ari4java.tools.http.NettyHttpClient;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.List;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionException;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Consumer;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class CallAriClaude {
//    private static final Logger logger = LoggerFactory.getLogger(CallServiceAriImpl.class);
//    private ARI ari;
//    private NettyHttpClient httpClient;
//    private final String applicationName = "phoneCleansing";
//
//    // Track active calls for management
//    private final ConcurrentHashMap<String, Channel> activeChannels = new ConcurrentHashMap<>();
//    private final ConcurrentHashMap<String, Bridge> activeBridges = new ConcurrentHashMap<>();
//
//    // Counters for statistics
//    private final AtomicInteger callsPlaced = new AtomicInteger(0);
//    private final AtomicInteger callsAnswered = new AtomicInteger(0);
//    private final AtomicInteger callsFailed = new AtomicInteger(0);
//
//    // Event handlers
//    private final Map<String, Set<Consumer<Event>>> eventHandlers = new HashMap<>();
//
//    /**
//     * Initialize and connect to Asterisk ARI
//     * @param host Asterisk server host
//     * @param user ARI username
//     * @param password ARI password
//     * @return CompletableFuture that completes when connection is established
//     */
//    public CompletableFuture<Void> initialize(String host, String user, String password) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//
//        try {
//            httpClient = new NettyHttpClient();
//            ari = ARI.build(host, "ari", user, password, AriVersion.IM_FEELING_LUCKY);
//            ari.setHttpClient(httpClient);
//
//            // Setup application connection and websocket
//            CompletableFuture<Void> futures = CompletableFuture.runAsync(() -> {
//                try {
//                    ari.applications().subscribe(applicationName, "channel:,bridge:,playback:").execute();
//                    logger.info("Successfully subscribed to application events");
//
//                    ari.events().eventWebsocket(applicationName).execute((lhe, event) -> {
//                        handleEvent(event);
//                    });
//
//                    logger.info("ARI connection established successfully");
//                } catch (Exception ex) {
//                    logger.error("Failed to subscribe to application events", ex);
//                    throw new CompletionException(ex);
//                }
//            });
//
//        } catch (Exception e) {
//            logger.error("Failed to establish ARI connection", e);
//            future.completeExceptionally(e);
//        }
//
//        return future;
//    }
//
//    /**
//     * Disconnects from ARI and cleans up resources
//     */
//    public void shutdown() {
//        // Hangup all active calls before shutting down
//        activeChannels.forEach((id, channel) -> {
//            try {
//                ari.channels().hangup(id).execute();
//            } catch (RestException e) {
//                logger.warn("Failed to hangup channel {} during shutdown", id, e);
//            }
//        });
//
//        // Destroy active bridges
//        activeBridges.forEach((id, bridge) -> {
//            try {
//                ari.bridges().destroy(id).execute();
//            } catch (RestException e) {
//                logger.warn("Failed to destroy bridge {} during shutdown", id, e);
//            }
//        });
//
//        if (httpClient != null) {
//            httpClient.destroy();
//            logger.info("ARI connection closed");
//        }
//    }
//
//    /**
//     * Make an outbound call asynchronously
//     * @param fromNumber The caller ID to show
//     * @param toNumber The number to call
//     * @param mediaContext Optional media context for tracking/reporting
//     * @return CompletableFuture that completes with the Channel ID once call is initiated
//     */
//    public CompletableFuture<String> makeCall(String fromNumber, String toNumber, String mediaContext) {
//        try {
//            // Create a unique channel variable to track this call
//            String callId = "call-" + System.currentTimeMillis() + "-" + callsPlaced.incrementAndGet();
//
//            // Start building the originate request
//            CompletableFuture<Channel> channelFuture = ari.channels().originate(
//                            "SIP/" + toNumber)
//                    .setCallerId(fromNumber)
//                    .setApp(applicationName)
//                    .setAppArgs(mediaContext, callId)
//                    .setOtherChannelVars(Map.of(
//                            "CALLER_ID", fromNumber,
//                            "MEDIA_CONTEXT", mediaContext != null ? mediaContext : "",
//                            "CALL_ID", callId
//                    ))
//                    .execute();
//
//            // Handle the channel once created
//            return channelFuture.thenApply(channel -> {
//                String channelId = channel.getId();
//                activeChannels.put(channelId, channel);
//                logger.info("Call initiated to {} with channel ID: {}", toNumber, channelId);
//                return channelId;
//            });
//        } catch (RestException e) {
//            logger.error("Failed to make call to " + toNumber, e);
//            callsFailed.incrementAndGet();
//            CompletableFuture<String> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//
//    /**
//     * Make multiple calls in parallel
//     * @param callRequests List of call requests with to/from numbers
//     * @return CompletableFuture that completes with a list of channel IDs
//     */
//    public CompletableFuture<List<String>> makeConcurrentCalls(List<CallRequest> callRequests) {
//        List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
//
//        // Start all calls concurrently
//        for (CallRequest request : callRequests) {
//            futures.add(makeCall(request.getFromNumber(), request.getToNumber(), request.getMediaContext()));
//        }
//
//        // Wait for all calls to complete
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenApply(v -> {
//                    // Collect all successful channel IDs
//                    return futures.stream()
//                            .map(future -> {
//                                try {
//                                    return future.getNow(null);
//                                } catch (Exception e) {
//                                    return null;
//                                }
//                            })
//                            .filter(java.util.Objects::nonNull)
//                            .collect(java.util.stream.Collectors.toList());
//                });
//    }
//
//    /**
//     * Play an announcement on a channel
//     * @param channelId The channel ID
//     * @param soundFile The sound file to play
//     * @return CompletableFuture with the Playback ID
//     */
//    public CompletableFuture<String> playAnnouncement(String channelId, String soundFile) {
//        try {
//            CompletableFuture<Playback> playbackFuture = (CompletableFuture<Playback>) ari.channels().play(channelId,
//                            "sound:" + soundFile)
//                    .execute();
//
//            return playbackFuture.thenApply(playback -> {
//                String playbackId = playback.getId();
//                logger.info("Playing announcement {} on channel {}, playback ID: {}",
//                        soundFile, channelId, playbackId);
//                return playbackId;
//            });
//        } catch (RestException e) {
//            logger.error("Failed to play announcement on channel " + channelId, e);
//            CompletableFuture<String> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//
//    /**
//     * Create a bridge for connecting multiple channels
//     * @param name Optional name for the bridge
//     * @return CompletableFuture with the Bridge ID
//     */
//    public CompletableFuture<String> createBridge(String name) {
//        try {
//            String bridgeName = name != null ? name : "bridge_" + System.currentTimeMillis();
//            CompletableFuture<Bridge> bridgeFuture = (CompletableFuture<Bridge>) ari.bridges()
//                    .create()
//                    .execute();
//
//            return bridgeFuture.thenApply(bridge -> {
//                String bridgeId = bridge.getId();
//                activeBridges.put(bridgeId, bridge);
//                logger.info("Bridge created with ID: {}", bridgeId);
//                return bridgeId;
//            });
//        } catch (RestException e) {
//            logger.error("Failed to create bridge", e);
//            CompletableFuture<String> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//
//    /**
//     * Add a channel to a bridge
//     * @param bridgeId The bridge ID
//     * @param channelId The channel ID to add
//     * @return CompletableFuture that completes when operation is done
//     */
//    public CompletableFuture<Void> addChannelToBridge(String bridgeId, String channelId) {
//        try {
//            return ari.bridges().addChannel(bridgeId, channelId).execute()
//                    .thenRun(() -> {
//                        logger.info("Added channel {} to bridge {}", channelId, bridgeId);
//                    });
//        } catch (RestException e) {
//            logger.error("Failed to add channel {} to bridge {}", channelId, bridgeId, e);
//            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//
//    /**
//     * Add multiple channels to a bridge concurrently
//     * @param bridgeId The bridge ID
//     * @param channelIds List of channel IDs to add
//     * @return CompletableFuture that completes when all channels are added
//     */
//    public CompletableFuture<Void> addChannelsToBridge(String bridgeId, List<String> channelIds) {
//        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
//
//        // Add all channels concurrently
//        for (String channelId : channelIds) {
//            futures.add(addChannelToBridge(bridgeId, channelId));
//        }
//
//        // Wait for all operations to complete
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//    }
//
//    /**
//     * Hang up a call
//     * @param channelId The channel ID to hang up
//     * @return CompletableFuture that completes when hangup is done
//     */
//    public CompletableFuture<Void> hangupCall(String channelId) {
//        try {
//            return ari.channels().hangup(channelId).execute()
//                    .thenRun(() -> {
//                        activeChannels.remove(channelId);
//                        logger.info("Channel {} has been hung up", channelId);
//                    });
//        } catch (RestException e) {
//            logger.error("Failed to hang up channel " + channelId, e);
//            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//
//    /**
//     * Hang up multiple calls concurrently
//     * @param channelIds List of channel IDs to hang up
//     * @return CompletableFuture that completes when all calls are hung up
//     */
//    public CompletableFuture<Void> hangupCalls(List<String> channelIds) {
//        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
//
//        // Hangup all calls concurrently
//        for (String channelId : channelIds) {
//            futures.add(hangupCall(channelId));
//        }
//
//        // Wait for all operations to complete
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//    }
//
//    /**
//     * Verify if a number is reachable asynchronously
//     * @param number The number to verify
//     * @param timeout Timeout in seconds
//     * @param onResult Callback to execute with result when verification completes
//     */
//    public void verifyNumber(String number, int timeout, Consumer<Boolean> onResult) {
//        try {
//            // Create a unique channel variable to track this verification
//            String verificationId = "verify-" + System.currentTimeMillis();
//
//            // Register a one-time state change handler for this verification
//            String eventKey = "verification_" + verificationId;
//            registerEventHandler(ChannelStateChange.class.getSimpleName(), eventKey, event -> {
//                ChannelStateChange stateChange = (ChannelStateChange) event;
//                String channelId = stateChange.getChannel().getId();
//                String state = stateChange.getChannel().getState();
//
//                boolean isReachable = "UP".equals(state) || "RINGING".equals(state);
//                logger.info("Verification call to {} state: {} (reachable: {})", number, state, isReachable);
//
//                // Clean up after verification
//                try {
//                    ari.channels().hangup(channelId).execute();
//                    unregisterEventHandler(ChannelStateChange.class.getSimpleName(), eventKey);
//                    onResult.accept(isReachable);
//                } catch (RestException e) {
//                    logger.error("Failed to hang up verification call", e);
//                }
//            });
//
//            // Start the verification call
//            ari.channels().originate("SIP/" + number)
//                    .setCallerId("Verification")
//                    .setTimeout(timeout)
//                    .setApp(applicationName)
//                    .setAppArgs("verification", verificationId)
//                    .execute()
//                    .thenAccept(channel -> {
//                        logger.info("Verification call to {} initiated with channel ID: {}", number, channel.getId());
//
//                        // Set a timeout to handle case where no state change occurs
//                        java.util.Timer timer = new java.util.Timer();
//                        timer.schedule(new java.util.TimerTask() {
//                            @Override
//                            public void run() {
//                                unregisterEventHandler(ChannelStateChange.class.getSimpleName(), eventKey);
//                                try {
//                                    ari.channels().hangup(channel.getId()).execute();
//                                    logger.info("Verification call to {} timed out", number);
//                                    onResult.accept(false);
//                                } catch (RestException e) {
//                                    logger.error("Failed to hang up verification call after timeout", e);
//                                }
//                            }
//                        }, timeout * 1000L);
//                    }).exceptionally(ex -> {
//                        logger.error("Failed to initiate verification call to " + number, ex);
//                        unregisterEventHandler(ChannelStateChange.class.getSimpleName(), eventKey);
//                        onResult.accept(false);
//                        return null;
//                    });
//        } catch (RestException e) {
//            logger.error("Failed to initiate verification call to " + number, e);
//            onResult.accept(false);
//        }
//    }
//
//    /**
//     * Verify multiple numbers concurrently
//     * @param numbers List of numbers to verify
//     * @param timeout Timeout in seconds
//     * @return CompletableFuture that completes with a Map of number to verification result
//     */
//    public CompletableFuture<Map<String, Boolean>> verifyNumbers(List<String> numbers, int timeout) {
//        CompletableFuture<Map<String, Boolean>> resultFuture = new CompletableFuture<>();
//        Map<String, Boolean> results = new ConcurrentHashMap<>();
//        AtomicInteger remaining = new AtomicInteger(numbers.size());
//
//        for (String number : numbers) {
//            verifyNumber(number, timeout, result -> {
//                results.put(number, result);
//                if (remaining.decrementAndGet() == 0) {
//                    resultFuture.complete(results);
//                }
//            });
//        }
//
//        return resultFuture;
//    }
//
//    /**
//     * Handle incoming events from Asterisk
//     * @param event The event to handle
//     */
//    private void handleEvent(Object event) {
//        String eventType = event.getClass().getSimpleName();
//        logger.debug("Received event: {}", eventType);
//
//        if (event instanceof StasisStart) {
//            StasisStart stasisStart = (StasisStart) event;
//            Channel channel = stasisStart.getChannel();
//            String channelId = channel.getId();
//
//            // Track the channel
//            activeChannels.put(channelId, channel);
//            logger.info("Channel {} entered application", channelId);
//
//            // Call state has changed to "UP" when entering Stasis
//            callsAnswered.incrementAndGet();
//        } else if (event instanceof ChannelStateChange) {
//            // Notify state change handlers
//            notifyEventHandlers(eventType, event);
//        } else if (event instanceof ChannelDtmfReceived) {
//            // Handle DTMF tones
//            ChannelDtmfReceived dtmfEvent = (ChannelDtmfReceived) event;
//            logger.info("DTMF {} received on channel {}",
//                    dtmfEvent.getDigit(), dtmfEvent.getChannel().getId());
//        }
//    }
//
//    /**
//     * Register an event handler
//     * @param eventType The event type to handle
//     * @param handlerId A unique ID for this handler
//     * @param handler The handler function to call
//     */
//    public void registerEventHandler(String eventType, String handlerId, Consumer<Event> handler) {
//        eventHandlers.computeIfAbsent(eventType, k -> new HashSet<>()).add(handler);
//    }
//
//    /**
//     * Unregister an event handler
//     * @param eventType The event type
//     * @param handlerId The handler ID to remove
//     */
//    public void unregisterEventHandler(String eventType, String handlerId) {
//        Set<Consumer<Event>> handlers = eventHandlers.get(eventType);
//        if (handlers != null) {
//            handlers.removeIf(handler -> handler.equals(handlerId));
//        }
//    }
//
//    /**
//     * Notify all handlers for an event type
//     * @param eventType The event type
//     * @param event The event object
//     */
//    private void notifyEventHandlers(String eventType, Event event) {
//        Set<Consumer<Event>> handlers = eventHandlers.get(eventType);
//        if (handlers != null) {
//            for (Consumer<Event> handler : handlers) {
//                try {
//                    handler.accept(event);
//                } catch (Exception e) {
//                    logger.error("Error in event handler for {}", eventType, e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Get all active channel IDs
//     * @return Set of active channel IDs
//     */
//    public Set<String> getActiveChannelIds() {
//        return activeChannels.keySet();
//    }
//
//    /**
//     * Get statistics about calls made
//     * @return Map with statistics
//     */
//    public Map<String, Integer> getCallStatistics() {
//        Map<String, Integer> stats = new HashMap<>();
//        stats.put("callsPlaced", callsPlaced.get());
//        stats.put("callsAnswered", callsAnswered.get());
//        stats.put("callsFailed", callsFailed.get());
//        stats.put("activeChannels", activeChannels.size());
//        stats.put("activeBridges", activeBridges.size());
//        return stats;
//    }
//
//    /**
//     * Class to represent a call request
//     */
//    public static class CallRequest {
//        private final String fromNumber;
//        private final String toNumber;
//        private final String mediaContext;
//
//        public CallRequest(String fromNumber, String toNumber, String mediaContext) {
//            this.fromNumber = fromNumber;
//            this.toNumber = toNumber;
//            this.mediaContext = mediaContext;
//        }
//
//        public String getFromNumber() {
//            return fromNumber;
//        }
//
//        public String getToNumber() {
//            return toNumber;
//        }
//
//        public String getMediaContext() {
//            return mediaContext;
//        }
//    }
//}