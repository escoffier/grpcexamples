package routeguide;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RouteGuideServer {
    private static final Logger logger = Logger.getLogger(RouteGuideServer.class.getName());

    private final int port;
    private final Server server;

    public RouteGuideServer(int port) throws IOException {
        this(port, RouteGuideUtil.getDefaultFeaturesFile());

    }

    public RouteGuideServer(int port, URL featureFile) throws IOException {
        this(ServerBuilder.forPort(port), port, RouteGuideUtil.parseFeatures(featureFile));
    }

    public RouteGuideServer(ServerBuilder<?> serverBuilder, int port, Collection<Feature> features) {
        this.port = port;
        server = serverBuilder.addService(new RouteGuideService(features)).build();
    }

    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RouteGuideServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        RouteGuideServer routeGuideServer = new RouteGuideServer(7860);
        routeGuideServer.start();
        routeGuideServer.blockUntilShutdown();
    }

    static class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {
        private final Collection<Feature> features;
        private final ConcurrentMap<Point, List<RouteNote>> routeNotes = new ConcurrentHashMap<>();

        RouteGuideService(Collection<Feature> features) {
            this.features = features;
        }

        @Override
        public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
            super.getFeature(request, responseObserver);
        }

        @Override
        public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
            super.listFeatures(request, responseObserver);
        }

        /**
         * Gets a stream of points, and responds with statistics about the "trip": number of points,
         * number of known features visited, total distance traveled, and total time spent.
         *
         * @param responseObserver an observer to receive the response summary.
         * @return an observer to receive the requested route points.
         */
        @Override
        public StreamObserver<Point> recordRoute(final StreamObserver<RouteSummary> responseObserver) {
            return new StreamObserver<Point>() {
                int pointCount;
                int featureCount;
                int distance;
                Point previous;
                final long startTime = System.nanoTime();

                @Override
                public void onNext(Point value) {
                    pointCount++;
                    if (RouteGuideUtil.exists(checkFeature(value))) {
                        featureCount++;
                    }
                    if (previous != null) {
                        distance += calcDistance(previous, value);
                    }
                    previous = value;
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "recordRoute cancelled");
                }

                @Override
                public void onCompleted() {
                    long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);

                    responseObserver.onNext(RouteSummary.newBuilder()
                            .setPointCount(pointCount)
                            .setFeatureCount(featureCount)
                            .setDistance(distance)
                            .setElapsedTime((int)seconds).build());

                    responseObserver.onCompleted();

                }
            };
            //return super.recordRoute(responseObserver);
        }

        @Override
        public StreamObserver<RouteNote> routeChat(StreamObserver<RouteNote> responseObserver) {
            return super.routeChat(responseObserver);
        }

        /**
         * Get the notes list for the given location. If missing, create it.
         */
        private List<RouteNote> getOrCreateNotes(Point location) {
            List<RouteNote> notes = Collections.synchronizedList(new ArrayList<RouteNote>());
            List<RouteNote> prevNotes = routeNotes.putIfAbsent(location, notes);
            return prevNotes != null ? prevNotes : notes;
        }

        /**
         * Gets the feature at the given point.
         *
         * @param location the location to check.
         * @return The feature object at the point. Note that an empty name indicates no feature.
         */
        private Feature checkFeature(Point location) {
            for (Feature feature : features) {
                if (feature.getLocation().getLatitude() == location.getLatitude()
                        && feature.getLocation().getLongitude() == location.getLongitude()) {
                    return feature;
                }
            }

            // No feature was found, return an unnamed feature.
            return Feature.newBuilder().setName("").setLocation(location).build();
        }

        /**
         * Calculate the distance between two points using the "haversine" formula.
         * The formula is based on http://mathforum.org/library/drmath/view/51879.html.
         *
         * @param start The starting point
         * @param end   The end point
         * @return The distance between the points in meters
         */
        private static int calcDistance(Point start, Point end) {
            int r = 6371000; // earth radius in meters
            double lat1 = toRadians(RouteGuideUtil.getLatitude(start));
            double lat2 = toRadians(RouteGuideUtil.getLatitude(end));
            double lon1 = toRadians(RouteGuideUtil.getLongitude(start));
            double lon2 = toRadians(RouteGuideUtil.getLongitude(end));
            double deltaLat = lat2 - lat1;
            double deltaLon = lon2 - lon1;

            double a = sin(deltaLat / 2) * sin(deltaLat / 2)
                    + cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2);
            double c = 2 * atan2(sqrt(a), sqrt(1 - a));

            return (int) (r * c);
        }
    }
}