package sr.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import sr.grpc.gen.event.*; // Import all generated classes
import sr.grpc.gen.event.EventSubscriptionServiceGrpc.EventSubscriptionServiceBlockingStub;
import sr.grpc.gen.event.EventSubscriptionServiceGrpc.EventSubscriptionServiceStub;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherEventSubscriptionClient {
    private static final Logger logger = Logger.getLogger(WeatherEventSubscriptionClient.class.getName());

    private final ManagedChannel channel;
    private final EventSubscriptionServiceStub asyncStub;
    private final EventSubscriptionServiceBlockingStub blockingStub;
    private final String clientInstanceId; // Identifier for this client instance

    // Optional: Map to keep track of active subscriptions initiated by this client instance
    // Key: client_subscription_id, Value: Description (e.g., city) or the StreamObserver itself
    private final ConcurrentMap<String, String> activeSubscriptions = new ConcurrentHashMap<>();

    public WeatherEventSubscriptionClient(String host, int port, String clientInstanceId) {
        // Add keepAlive settings matching server expectations (optional but recommended)
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Disable TLS/SSL for local testing
                .keepAliveTime(60, TimeUnit.SECONDS) // Should be >= server keepAliveTime
                .keepAliveTimeout(30, TimeUnit.SECONDS) // Should be >= server keepAliveTimeout
                .keepAliveWithoutCalls(true)
                .build();

        this.asyncStub = EventSubscriptionServiceGrpc.newStub(channel);
        this.blockingStub = EventSubscriptionServiceGrpc.newBlockingStub(channel);
        this.clientInstanceId = clientInstanceId;
        logger.info("Client instance " + clientInstanceId + " created for " + host + ":" + port);
    }

    public void shutdown() throws InterruptedException {
        logger.info("Shutting down client instance: " + clientInstanceId);
        // Optional: Gracefully unsubscribe from all active subscriptions before shutting down
        if (!activeSubscriptions.isEmpty()) {
            logger.info("Unsubscribing from " + activeSubscriptions.size() + " active subscriptions before shutdown...");
            activeSubscriptions.keySet().forEach(this::unsubscribe); // Call unsubscribe for each known ID
            activeSubscriptions.clear(); // Clear the map
            TimeUnit.MILLISECONDS.sleep(500); // Give time for unsubscribe RPCs
        }

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        if (!channel.isTerminated()) {
            logger.warning("Channel did not terminate gracefully after 5 seconds for client: " + clientInstanceId);
            channel.shutdownNow();
        }
        logger.info("Client instance " + clientInstanceId + " shut down.");
    }

    /**
     * Subscribes to events of a specific type and criteria.
     *
     * @param eventType The type of event (e.g., WEATHER_UPDATE).
     * @param criteria  The subscription criteria (e.g., target_identifier="Kraków").
     * @return The unique client_subscription_id generated for this subscription. Null if subscription fails immediately.
     */
    public String subscribe(EventType eventType, SubscriptionCriteria criteria) {
        // Generate a unique ID for this subscription attempt
        String clientSubscriptionId = clientInstanceId + "-" + eventType.name() + "-" + UUID.randomUUID();
        String description = criteria.getTargetIdentifier(); // Use target_identifier as description

        logger.info("[" + clientInstanceId + "] Subscribing with ID: " + clientSubscriptionId +
                " | Type: " + eventType + " | Criteria: " + description);

        SubscriptionRequest request = SubscriptionRequest.newBuilder()
                .setClientSubscriptionId(clientSubscriptionId)
                .setEventType(eventType)
                .setCriteria(criteria)
                .build();

        // Use a CountDownLatch to wait for the initial stream setup (optional, for simpler main logic)
        CountDownLatch setupLatch = new CountDownLatch(1);
        final Status[] errorStatus = {Status.OK}; // Array to hold error status from observer thread

        asyncStub.subscribe(request, new StreamObserver<EventNotification>() {
            @Override
            public void onNext(EventNotification notification) {
                // Check if it's the confirmation message (optional check)
                if (notification.getEventPayloadCase() == EventNotification.EventPayloadCase.EVENTPAYLOAD_NOT_SET &&
                        notification.getNotificationId().startsWith("CONFIRM-")) {
                    logger.info("[" + clientInstanceId + "][SubID: " + notification.getClientSubscriptionId() +
                            "] Subscription confirmed by server.");
                    // Store the subscription ID locally upon confirmation
                    activeSubscriptions.put(clientSubscriptionId, description);
                    setupLatch.countDown(); // Signal successful setup
                } else {
                    // Process actual event notification
                    handleEventNotification(notification);
                }
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                        "] Subscription stream error: " + status.getCode() + " - " + status.getDescription(), t);
                activeSubscriptions.remove(clientSubscriptionId); // Remove if stream fails
                errorStatus[0] = status; // Store error
                setupLatch.countDown(); // Signal failure/completion
            }

            @Override
            public void onCompleted() {
                logger.info("[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                        "] Subscription stream completed by server.");
                activeSubscriptions.remove(clientSubscriptionId); // Remove on completion
                setupLatch.countDown(); // Signal completion
            }
        });

        // Wait briefly for the stream setup (or immediate error)
        try {
            if (!setupLatch.await(5, TimeUnit.SECONDS)) {
                logger.warning("[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                        "] Timed out waiting for subscription confirmation/error.");
                // Consider cancelling the RPC here if necessary, though it might already be problematic
                return null; // Indicate setup timeout
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                    "] Interrupted while waiting for subscription setup.");
            return null; // Indicate interruption
        }

        // Check if an error occurred during setup
        if (!errorStatus[0].isOk()) {
            logger.severe("[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                    "] Subscription failed during setup: " + errorStatus[0]);
            return null; // Indicate setup failure
        }

        // Check if the subscription was actually added (possible race condition if onError happened fast)
        if (!activeSubscriptions.containsKey(clientSubscriptionId)) {
            // This might happen if onError was called before the onNext confirmation
            logger.warning("[" + clientInstanceId + "][SubID: " + clientSubscriptionId +
                    "] Subscription might not have been fully established despite no immediate error.");
            // Don't return the ID if it wasn't successfully stored.
            // It implies the stream failed very early or the confirmation was missed.
            return null;
        }


        return clientSubscriptionId; // Return the ID if setup seems successful
    }

    // Helper method to process different notification types
    private void handleEventNotification(EventNotification notification) {
        String subId = notification.getClientSubscriptionId();
        switch (notification.getEventType()) {
            case WEATHER_UPDATE:
                if (notification.getEventPayloadCase() == EventNotification.EventPayloadCase.WEATHER_UPDATE) {
                    WeatherUpdate update = notification.getWeatherUpdate();
                    System.out.println("[" + clientInstanceId + "] Pogoda dla " + update.getCity() + ":");
                    System.out.println("  Temperatura: " + String.format("%.1f", update.getCurrentTemperatureCelsius()) + " stopni Celsjusza");
                    System.out.println("  Warunki: " + update.getCurrentCondition());
                    System.out.println("  Wiatr: " + update.getWindSpeedKph() + " km/h");
                    // Możesz dodać więcej szczegółów, np. prognozę
                    if (!update.getForecastList().isEmpty()) {
                        System.out.println("  Prognoza:");
                        update.getForecastList().forEach(forecast -> {
                            System.out.println("    " + forecast.getDayDescription() + ": " + forecast.getCondition() + ", " +
                                    String.format("%.1f", forecast.getMaxTemperatureCelsius()) + "C / " +
                                    String.format("%.1f", forecast.getMinTemperatureCelsius()) + "C");
                        });
                    }
                } else {
                    logger.warning("[" + clientInstanceId + "][SubID: " + subId + "] Received WEATHER_UPDATE type with unexpected payload: " + notification.getEventPayloadCase());
                }
                break;
            case CONCERT_ALERT:
                // Handle concert alerts similarly
                logger.info("[" + clientInstanceId + "][SubID: " + subId + "] Received Concert Alert: " + notification.getConcertAlert().getArtist());
                break;
            case NEWS_FLASH:
                // Handle news flashes
                logger.info("[" + clientInstanceId + "][SubID: " + subId + "] Received News Flash: " + notification.getNewsFlash().getHeadline());
                break;
            case EVENT_TYPE_UNSPECIFIED:
            default:
                logger.warning("[" + clientInstanceId + "][SubID: " + subId + "] Received notification with unknown or unspecified event type: " + notification.getEventType());
                break;
        }
    }


    /**
     * Unsubscribes from a specific subscription using its ID.
     *
     * @param subscriptionId The client_subscription_id returned by subscribe().
     * @return true if the unsubscription RPC was successful according to the server, false otherwise.
     */
    public boolean unsubscribe(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            logger.warning("[" + clientInstanceId + "] Cannot unsubscribe with null or empty subscription ID.");
            return false;
        }
        logger.info("[" + clientInstanceId + "] Unsubscribing from ID: " + subscriptionId);
        UnsubscriptionRequest request = UnsubscriptionRequest.newBuilder()
                .setClientSubscriptionId(subscriptionId)
                .build();

        try {
            // Use blocking stub for simple unary RPC
            UnsubscriptionResponse response = blockingStub.unsubscribe(request);
            if (response.getSuccess()) {
                logger.info("[" + clientInstanceId + "] Unsubscription successful for ID: " + subscriptionId + " - Server message: " + response.getMessage());
                activeSubscriptions.remove(subscriptionId); // Remove from local tracking
                return true;
            } else {
                logger.warning("[" + clientInstanceId + "] Unsubscription failed for ID: " + subscriptionId + " - Server message: " + response.getMessage());
                // Optionally remove from activeSubscriptions even on failure if server says ID not found?
                // activeSubscriptions.remove(subscriptionId);
                return false;
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.SEVERE, "[" + clientInstanceId + "] Unsubscription RPC failed for ID: " + subscriptionId, e);
            // If RPC fails, the subscription might still be active on the server (or already gone).
            // Consider removing from local map anyway? Or retry? For simplicity, we'll just log.
            // activeSubscriptions.remove(subscriptionId);
            return false;
        }
    }

    // --- Main Method for Demonstration ---
    public static void main(String[] args) {
        String host = "localhost";
        int port = 50051;
        String client1Id = "Client-Alpha";
        String client2Id = "Client-Beta";

        WeatherEventSubscriptionClient client1 = new WeatherEventSubscriptionClient(host, port, client1Id);
        WeatherEventSubscriptionClient client2 = new WeatherEventSubscriptionClient(host, port, client2Id);

        String sub1_krakow_id = null;
        String sub1_warszawa_id = null;
        String sub2_warszawa_id = null;

        try {
            // Client 1 subscribes to Kraków weather
            SubscriptionCriteria krakowCriteria = SubscriptionCriteria.newBuilder().setTargetIdentifier("Kraków").build();
            sub1_krakow_id = client1.subscribe(EventType.WEATHER_UPDATE, krakowCriteria);
            if (sub1_krakow_id == null) {
                logger.severe(client1Id + " failed to subscribe to Kraków weather.");
                // Handle failure, maybe exit or skip related steps
            }

            // Client 1 subscribes to Warszawa weather
            SubscriptionCriteria warszawaCriteria = SubscriptionCriteria.newBuilder().setTargetIdentifier("Warszawa").build();
            sub1_warszawa_id = client1.subscribe(EventType.WEATHER_UPDATE, warszawaCriteria);
            if (sub1_warszawa_id == null) {
                logger.severe(client1Id + " failed to subscribe to Warszawa weather.");
            }


            // Client 2 subscribes to Warszawa weather
            sub2_warszawa_id = client2.subscribe(EventType.WEATHER_UPDATE, warszawaCriteria);
            if (sub2_warszawa_id == null) {
                logger.severe(client2Id + " failed to subscribe to Warszawa weather.");
            }


            // Let the subscriptions run and receive notifications for a while
            logger.info("--- Waiting for notifications (15 seconds) ---");
            TimeUnit.SECONDS.sleep(15);

            // Client 1 unsubscribes from Kraków
            logger.info("--- Client 1 unsubscribing from Kraków ---");
            if (sub1_krakow_id != null) {
                client1.unsubscribe(sub1_krakow_id);
            } else {
                logger.warning(client1Id + " has no valid ID to unsubscribe from Kraków.");
            }


            // Wait a bit more
            logger.info("--- Waiting for more notifications (5 seconds) ---");
            TimeUnit.SECONDS.sleep(5);

            // Client 1 unsubscribes from Warszawa
            logger.info("--- Client 1 unsubscribing from Warszawa ---");
            if (sub1_warszawa_id != null) {
                client1.unsubscribe(sub1_warszawa_id);
            } else {
                logger.warning(client1Id + " has no valid ID to unsubscribe from Warszawa.");
            }

            // Client 2 is still subscribed to Warszawa...

            logger.info("--- Waiting for final notifications (5 seconds) ---");
            TimeUnit.SECONDS.sleep(5);


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Main thread interrupted!");
        } finally {
            // Shutdown clients gracefully
            logger.info("--- Shutting down clients ---");
            try {
                client1.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Failed to shutdown " + client1Id, e);
            }
            try {
                client2.shutdown(); // Will automatically unsubscribe from its Warszawa subscription
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Failed to shutdown " + client2Id, e);
            }
        }

        logger.info("--- Client demonstration finished ---");
    }
}