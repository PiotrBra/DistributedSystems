package sr.grpc.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import sr.grpc.gen.event.*;
import sr.grpc.gen.event.EventSubscriptionServiceGrpc.EventSubscriptionServiceImplBase;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WeatherEventSubscriptionService extends EventSubscriptionServiceImplBase {

    private static final Logger logger = Logger.getLogger(WeatherEventSubscriptionService.class.getName());

    // Stores active subscriptions: client_subscription_id -> SubscriptionInfo
    private final ConcurrentMap<String, SubscriptionInfo> activeSubscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService eventGeneratorScheduler;
    private final Random random = new Random();

    // Helper class to store observer and criteria together
    private static class SubscriptionInfo {
        final StreamObserver<EventNotification> observer;
        final SubscriptionRequest request; // Store the full request for easy access to criteria and type
        final String clientSubscriptionId;

        SubscriptionInfo(String clientSubscriptionId, SubscriptionRequest request, StreamObserver<EventNotification> observer) {
            this.clientSubscriptionId = clientSubscriptionId;
            this.request = request;
            this.observer = observer;
        }
    }

    // Constructor accepting the scheduler
    public WeatherEventSubscriptionService(ScheduledExecutorService eventGeneratorScheduler) {
        this.eventGeneratorScheduler = eventGeneratorScheduler;
    }

    // Method to start the periodic event generation
    public void startEventGeneration(long initialDelay, long period, TimeUnit unit) {
        eventGeneratorScheduler.scheduleAtFixedRate(this::generateAndSendNotifications, initialDelay, period, unit);
        logger.info("Event generation scheduled: initialDelay=" + initialDelay + ", period=" + period + " " + unit.name());
    }

    @Override
    public void subscribe(SubscriptionRequest request, StreamObserver<EventNotification> responseObserver) {
        String clientSubscriptionId = request.getClientSubscriptionId();
        EventType eventType = request.getEventType();
        SubscriptionCriteria criteria = request.getCriteria();

        // Basic validation
        if (clientSubscriptionId == null || clientSubscriptionId.trim().isEmpty()) {
            logger.warning("Subscription attempt with empty client_subscription_id");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("client_subscription_id cannot be empty")
                    .asRuntimeException());
            return;
        }
        if (eventType == EventType.EVENT_TYPE_UNSPECIFIED) {
            logger.warning("Subscription attempt with unspecified event type for ID: " + clientSubscriptionId);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("event_type must be specified")
                    .asRuntimeException());
            return;
        }
        // Add more specific criteria validation if needed (e.g., city required for WEATHER_UPDATE)
        if (eventType == EventType.WEATHER_UPDATE && (criteria.getTargetIdentifier() == null || criteria.getTargetIdentifier().trim().isEmpty())) {
            logger.warning("Weather subscription attempt without target_identifier (city) for ID: " + clientSubscriptionId);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("target_identifier (city) is required for WEATHER_UPDATE")
                    .asRuntimeException());
            return;
        }

        // Cast to ServerCallStreamObserver to handle cancellation
        final ServerCallStreamObserver<EventNotification> serverObserver =
                (ServerCallStreamObserver<EventNotification>) responseObserver;

        // Create subscription info
        final SubscriptionInfo subInfo = new SubscriptionInfo(clientSubscriptionId, request, serverObserver);

        // Attempt to add the subscription atomically
        SubscriptionInfo previous = activeSubscriptions.putIfAbsent(clientSubscriptionId, subInfo);

        if (previous != null) {
            // Subscription ID already exists
            logger.warning("Subscription attempt with duplicate client_subscription_id: " + clientSubscriptionId);
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Subscription ID '" + clientSubscriptionId + "' is already in use.")
                    .asRuntimeException());
            return;
        }

        // Set a handler for when the client cancels the stream
        serverObserver.setOnCancelHandler(() -> {
            logger.info("Client cancelled subscription: " + clientSubscriptionId);
            activeSubscriptions.remove(clientSubscriptionId); // Clean up map on cancellation
        });

        logger.info("Client subscribed: ID=" + clientSubscriptionId + ", Type=" + eventType + ", Criteria=" + criteria.getTargetIdentifier());

        // Send a confirmation notification (optional)
        try {
            EventNotification confirmation = EventNotification.newBuilder()
                    .setNotificationId("CONFIRM-" + System.currentTimeMillis())
                    .setClientSubscriptionId(clientSubscriptionId)
                    .setTimestampUnixSeconds(Instant.now().getEpochSecond())
                    .setEventType(eventType) // Echo back the type
                    // No payload for confirmation, or add a specific confirmation message
                    .build();
            serverObserver.onNext(confirmation);
        } catch (StatusRuntimeException e) {
            // Handle cases where sending confirmation fails (e.g., client disconnected immediately)
            logger.log(Level.WARNING, "Failed to send subscription confirmation for ID: " + clientSubscriptionId + ", removing subscription.", e);
            activeSubscriptions.remove(clientSubscriptionId);
        }

        // Note: We don't call onCompleted here because the stream stays open for notifications.
        // It will be completed either by the client cancelling, the server explicitly calling
        // onCompleted/onError, or network issues causing errors during onNext.
    }

    @Override
    public void unsubscribe(UnsubscriptionRequest request, StreamObserver<UnsubscriptionResponse> responseObserver) {
        String clientSubscriptionId = request.getClientSubscriptionId();
        logger.info("Received unsubscribe request for ID: " + clientSubscriptionId);

        SubscriptionInfo removedSubscription = activeSubscriptions.remove(clientSubscriptionId);

        boolean success = removedSubscription != null;
        String message;

        if (success) {
            message = "Unsubscribed successfully from " + clientSubscriptionId;
            logger.info(message);
            // Optionally, gracefully close the stream from the server side
            try {
                removedSubscription.observer.onCompleted();
            } catch (Exception e) {
                // Log error if closing stream fails, but unsubscription from map succeeded.
                logger.log(Level.WARNING, "Error closing stream on unsubscribe for ID: " + clientSubscriptionId, e);
            }
        } else {
            message = "Subscription ID not found: " + clientSubscriptionId;
            logger.warning(message);
        }

        UnsubscriptionResponse response = UnsubscriptionResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Main logic for generating and distributing notifications
    private void generateAndSendNotifications() {
        // Simulate generating one type of event
        // In a real app, you might have multiple generators or sources
        try {
            generateAndSendWeatherUpdates();
            // generateAndSendConcertAlerts(); // etc.
        } catch (Exception e) {
            // Catch unexpected errors in generation logic
            logger.log(Level.SEVERE, "Error during event generation cycle", e);
        }
    }

    private void generateAndSendWeatherUpdates() {
        // --- Generate a sample WeatherUpdate event ---
        String[] cities = {"Kraków", "Warszawa", "Gdańsk", "Wrocław", "Poznań"};
        String city = cities[random.nextInt(cities.length)];
        WeatherUpdate weatherUpdate = generateRandomWeatherUpdate(city);

        EventNotification notification = EventNotification.newBuilder()
                .setNotificationId("WEATHER-" + System.currentTimeMillis() + "-" + city)
                .setTimestampUnixSeconds(Instant.now().getEpochSecond())
                .setEventType(EventType.WEATHER_UPDATE)
                .setWeatherUpdate(weatherUpdate)
                // client_subscription_id is set per subscriber below
                .build();

        // --- Distribute to relevant subscribers ---
        List<String> clientsToRemove = new ArrayList<>(); // To avoid ConcurrentModificationException

        for (Map.Entry<String, SubscriptionInfo> entry : activeSubscriptions.entrySet()) {
            String subId = entry.getKey();
            SubscriptionInfo subInfo = entry.getValue();

            // Check if this subscriber is interested in this event type and criteria
            if (subInfo.request.getEventType() == EventType.WEATHER_UPDATE &&
                    subInfo.request.getCriteria().getTargetIdentifier().equalsIgnoreCase(city))
            {
                // Build the notification specific to this subscriber
                EventNotification specificNotification = notification.toBuilder()
                        .setClientSubscriptionId(subId) // Set the correct ID for this client
                        .build();
                try {
                    // Send the notification
                    subInfo.observer.onNext(specificNotification);
                    logger.finest("Sent weather update for " + city + " to " + subId);
                } catch (StatusRuntimeException e) {
                    // Common gRPC errors: CANCELLED (client closed), UNAVAILABLE (network issues)
                    logger.log(Level.WARNING, "Failed to send notification to " + subId + " (Status: " + e.getStatus().getCode() + "). Marking for removal.", e.getMessage());
                    clientsToRemove.add(subId); // Mark for removal after iteration
                } catch (Exception e) {
                    // Catch other unexpected errors during sending
                    logger.log(Level.SEVERE, "Unexpected error sending notification to " + subId + ". Marking for removal.", e);
                    clientsToRemove.add(subId); // Mark for removal
                }
            }
        }

        // Remove subscriptions that failed (disconnected clients)
        if (!clientsToRemove.isEmpty()) {
            logger.info("Removing " + clientsToRemove.size() + " disconnected or errored subscriptions.");
            clientsToRemove.forEach(activeSubscriptions::remove);
        }
    }


    private WeatherUpdate generateRandomWeatherUpdate(String city) {
        // Get all const enum WeatherCondition
        WeatherCondition[] allConditions = WeatherCondition.values();

        // Filter invalid values (UNSPECIFIED and UNRECOGNIZED)
        List<WeatherCondition> validConditions = Arrays.stream(allConditions)
                .filter(wc -> wc != WeatherCondition.CONDITION_UNSPECIFIED && wc != WeatherCondition.UNRECOGNIZED)
                .collect(Collectors.toList());

        if (validConditions.isEmpty()) {
            logger.severe("Brak poprawnych wartości enum WeatherCondition do wylosowania!");
            // Return a default object or throw an exception to avoid further errors
            // Zwrócenie obiektu z opisem błędu jest bezpieczniejsze niż null
            return WeatherUpdate.newBuilder().setCity(city)
                    .setDetailedDescription("Błąd: Nie można wygenerować danych pogodowych - brak poprawnych warunków.")
                    .build();
        }

        // Pick the correct condition for the current weather
        WeatherCondition currentCondition = validConditions.get(random.nextInt(validConditions.size()));

        List<DailyForecast> forecastList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            WeatherCondition forecastCondition = validConditions.get(random.nextInt(validConditions.size()));

            forecastList.add(DailyForecast.newBuilder()
                    .setDayDescription("Dzień +" + (i + 1))
                    .setMaxTemperatureCelsius(random.nextDouble() * 15 + 10) // 10 to 25 C
                    .setMinTemperatureCelsius(random.nextDouble() * 10 + 5)  // 5 to 15 C
                    .setCondition(forecastCondition)
                    .setSummary("Losowa prognoza na dzień " + (i + 1))
                    .build());
        }

        return WeatherUpdate.newBuilder()
                .setCity(city)
                .setCurrentTemperatureCelsius(random.nextDouble() * 10 + 15) // 15 to 25 C
                .setHumidityPercent(random.nextDouble() * 50 + 30)       // 30 to 80%
                .setWindSpeedKph(random.nextInt(30))                 // 0 to 29 kph
                .setCurrentCondition(currentCondition)
                .setDetailedDescription("Losowe szczegóły pogodowe dla " + city + " o " + Instant.now())
                .addAllForecast(forecastList)
                .build();
    }

}