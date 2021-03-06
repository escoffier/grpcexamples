package routeguide;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Math.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {
    private static Logger logger = LoggerFactory.getLogger(RouteGuideService.class);

    //A feature names something at a given point.
    private final Collection<Feature> features;
    private final ConcurrentMap<Point, List<RouteNote>> routeNotes = new ConcurrentHashMap<>();

    public RouteGuideService(URL featureFile) throws IOException {
        this(RouteGuideUtil.parseFeatures(featureFile));
    }

    public RouteGuideService(Collection<Feature> features) {
        this.features = features;
    }

    @Override
    public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
        //super.getFeature(request, responseObserver);
        logger.info("######## " + responseObserver.toString());
        responseObserver.onNext(checkFeature(request));
//        responseObserver.onCompleted();
        responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("server error").withCause(new IOException("server exception"))));
    }

    @Override
    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
        //super.listFeatures(request, responseObserver);
        int left = min(request.getLo().getLongitude(), request.getHi().getLongitude());
        int right = max(request.getLo().getLongitude(), request.getHi().getLongitude());
        int top = max(request.getLo().getLatitude(), request.getHi().getLatitude());
        int bottom = min(request.getLo().getLatitude(), request.getHi().getLatitude());

        for (Feature feature : features) {
            if (!RouteGuideUtil.exists(feature)) {
                continue;
            }

            int lat = feature.getLocation().getLatitude();
            int lon = feature.getLocation().getLongitude();
            if (lon >= left && lon <= right && lat >= bottom && lat <= top) {
                responseObserver.onNext(feature);
            }
        }
        responseObserver.onCompleted();
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
                logger.info("Get points: " + value.toString());
            }

            @Override
            public void onError(Throwable t) {
                logger.info("recordRoute cancelled");
            }

            @Override
            public void onCompleted() {
                long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);

                logger.info(" send response");
                responseObserver.onNext(RouteSummary.newBuilder()
                        .setPointCount(pointCount)
                        .setFeatureCount(featureCount)
                        .setDistance(distance)
                        .setElapsedTime((int) seconds).build());

                responseObserver.onCompleted();
            }
        };
        //return super.recordRoute(responseObserver);
    }

    @Override
    public StreamObserver<RouteNote> routeChat(final StreamObserver<RouteNote> responseObserver) {
        //return super.routeChat(responseObserver);
        return new StreamObserver<RouteNote>() {
            @Override
            public void onNext(RouteNote note) {
                List<RouteNote> notes = getOrCreateNotes(note.getLocation());
//
//                    // Respond with all previous notes at this location.
                for (RouteNote prevNote : notes.toArray(new RouteNote[0])) {
                    responseObserver.onNext(prevNote);
                }
                notes.add(note);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("routeChat cancelled");
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();

            }
        };
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
