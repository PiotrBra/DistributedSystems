package sr.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WeatherEventSubscriptionServer {

    private static final Logger logger = Logger.getLogger(WeatherEventSubscriptionServer.class.getName());

    private final int port;
    private final Server server;
    private final WeatherEventSubscriptionService subscriptionService;
    private final ScheduledExecutorService eventGeneratorScheduler;

    public WeatherEventSubscriptionServer(int port, int threadPoolSize, long keepAliveSeconds) throws IOException {
        this.port = port;
        // Use a fixed thread pool for handling gRPC requests
        ThreadPoolExecutor grpcExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);

        // Separate scheduler for event generation
        this.eventGeneratorScheduler = Executors.newSingleThreadScheduledExecutor();
        this.subscriptionService = new WeatherEventSubscriptionService(eventGeneratorScheduler);

        // Configure server with keep-alive and the service implementation
        ServerBuilder<?> serverBuilder = NettyServerBuilder.forPort(port) // Use NettyServerBuilder for keepAlive
                .addService(subscriptionService)
                .executor(grpcExecutor) // Assign the executor for handling calls
                .keepAliveTime(keepAliveSeconds, TimeUnit.SECONDS) // Ping clients if idle
                .keepAliveTimeout(keepAliveSeconds / 2, TimeUnit.SECONDS) // Time to wait for ACK after ping
                .permitKeepAliveWithoutCalls(true); // Allow keepalive even if there are no ongoing calls

        this.server = serverBuilder.build();
        logger.info("Server configured on port " + port + " with keepAlive=" + keepAliveSeconds + "s");
    }

    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);

        // Start the event generator simulation
        subscriptionService.startEventGeneration(1, 3, TimeUnit.SECONDS); // start after 1s, repeat every 3s

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** Shutting down gRPC server due to JVM shutdown ***");
            try {
                this.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                System.err.println("Shutdown interrupted: " + e.getMessage());
            }
            System.err.println("*** Server shut down complete ***");
        }));
    }

    public void stop() throws InterruptedException {
        logger.info("Attempting graceful server shutdown...");

        // 1. Stop accepting new connections
        if (server != null && !server.isShutdown()) {
            server.shutdown();
        }

        // 2. Stop the event generator
        logger.info("Shutting down event generator...");
        eventGeneratorScheduler.shutdown();
        try {
            if (!eventGeneratorScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Event generator did not terminate gracefully, forcing shutdown.");
                eventGeneratorScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while waiting for event generator shutdown.");
            eventGeneratorScheduler.shutdownNow();
        }
        logger.info("Event generator shut down.");

        // 3. Wait for existing calls to finish or timeout
        if (server != null) {
            logger.info("Waiting for existing connections to terminate...");
            if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warning("Server did not terminate gracefully after 30 seconds, forcing shutdown.");
                server.shutdownNow(); // Forcefully terminate connections
                // Wait a bit more after forceful shutdown
                server.awaitTermination(5, TimeUnit.SECONDS);
            }
        }
        logger.info("Server shutdown process finished.");
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) {
        // Configuration parameters
        int port = 50051;
        int threadPoolSize = 10; // Number of threads to handle gRPC requests
        long keepAliveSeconds = 60; // Send keep-alive pings every 60 seconds

        try {
            WeatherEventSubscriptionServer server = new WeatherEventSubscriptionServer(port, threadPoolSize, keepAliveSeconds);
            server.start();
            server.blockUntilShutdown();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server failed to start", e);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            logger.log(Level.SEVERE, "Server interrupted during startup or blocking", e);
            System.exit(1);
        }
    }
}