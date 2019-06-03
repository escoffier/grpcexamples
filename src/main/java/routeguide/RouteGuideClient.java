package routeguide;

import com.google.common.net.HostAndPort;
import com.google.protobuf.Message;
import io.grpc.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
//import java.util.logging.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteGuideClient {
    private static final Logger logger = LoggerFactory.getLogger(RouteGuideClient.class);

    private  ManagedChannel channel;
    private  RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private  RouteGuideGrpc.RouteGuideStub asyncStub;
    private LoadBalancer loadBalancer;

    private Random random = new Random();
    private TestHelper testHelper;

    public RouteGuideClient(){

    }

    public RouteGuideClient( LoadBalancer loadBalancer) {
        //this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
        this.loadBalancer = loadBalancer;
    }

    public RouteGuideClient(String host, int port) {
        this(ManagedChannelBuilder
                //.forAddress(host, port)
                .forTarget("consul://"+ host+ ":" + port)
                .defaultLoadBalancingPolicy("round_robin")
                .nameResolverFactory(new ConsulNameResolver.ConsulNameResolverProvider("RouteGuideServer", 10, false, null))
                .usePlaintext());
    }
    /** Construct client for accessing RouteGuide server at {@code host:port}. */
    public RouteGuideClient(String host, int port, LoadBalancer loadBalancer) {
        this(ManagedChannelBuilder
                .forAddress(host, port)
                //.forTarget("consul://"+ host+ port)
                //.defaultLoadBalancingPolicy("")
                //.nameResolverFactory(new ConsulNameResolver.ConsulNameResolverProvider())
                .usePlaintext());
        //LoadBalancerRegistry.getDefaultRegistry();
        this.loadBalancer = loadBalancer;
    }

    /** Construct client for accessing RouteGuide server using the existing channel. */
    public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        //channel.
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
//        try {
//            //loadBalancer = new LoadBalancer("192.168.1.215", 8500);
//            this.loadBalancer = l
//            HostAndPort hostAndPort = loadBalancer.getService("RouteGuideServer");
//            info(hostAndPort.toString());
//            loadBalancer.subscibe("RouteGuideServer");
//        } catch (Exception ex) {
//            info(ex.getMessage());
//        }
    }

    public void setChannel(ManagedChannel channel) {
        this.channel = channel;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void info(String msg, Object... params) {
        //logger.info(Level.INFO, msg, params);
    }

    private void warning(String msg, Object... params) {
        //logger.info(Level.WARNING, msg, params);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public void getFeature(int lat, int lon) {
        //info("*** GetFeature: lat={0} lon={1}", lat, lon);
        logger.info("############ GetFeature:" + lat + ", " + lon);

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        Feature feature;
        try {
            feature = blockingStub.getFeature(request);
            if (testHelper != null) {
                testHelper.onMessage(feature);
            }
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            if (testHelper != null) {
                testHelper.onRpcError(e);
            }
            return;
        }
        logFeature(feature);
//        if (RouteGuideUtil.exists(feature)) {
//            info("Found feature called \"{0}\" at {1}, {2}",
//                    feature.getName(),
//                    RouteGuideUtil.getLatitude(feature.getLocation()),
//                    RouteGuideUtil.getLongitude(feature.getLocation()));
//        } else {
//            info("Found no feature at {0}, {1}",
//                    RouteGuideUtil.getLatitude(feature.getLocation()),
//                    RouteGuideUtil.getLongitude(feature.getLocation()));
//        }
    }

    private void logFeature(Feature feature) {
        if (RouteGuideUtil.exists(feature)) {
//            info("Found feature called \"{0}\" at {1}, {2}",
//                    feature.getName(),
//                    RouteGuideUtil.getLatitude(feature.getLocation()),
//                    RouteGuideUtil.getLongitude(feature.getLocation()));

            logger.info("Found feature called "+ feature.getName() +
                    " at " +
                    RouteGuideUtil.getLatitude(feature.getLocation()) + ", " +
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
//            info("Found no feature at {0}, {1}",
//                    RouteGuideUtil.getLatitude(feature.getLocation()),
//                    RouteGuideUtil.getLongitude(feature.getLocation()));
            logger.info("Found no feature at "+
                    RouteGuideUtil.getLatitude(feature.getLocation()) +
                    ", " +
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        }
    }

    public void getFeaturesAsync(int lat, int lon, CountDownLatch latch) {
        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
        //StreamObserver<Feature> streamObserver = new StreamObserver<Feature>
        asyncStub.getFeature( request, new StreamObserver<Feature>() {
            @Override
            public void onNext(Feature feature) {
                logFeature(feature);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("onError : "+ t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("getFeaturesAsync onCompleted");
                latch.countDown();
            }
        });
    }

    /**
     * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
     * response feature as it arrives.
     */
    public void listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        info("*** ListFeatures: lowLat={0} lowLon={1} hiLat={2} hiLon={3}", lowLat, lowLon, hiLat,
                hiLon);

        Rectangle request =
                Rectangle.newBuilder()
                        .setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
                        .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build()).build();
        Iterator<Feature> features;
        try {
            features = blockingStub.listFeatures(request);

            for (int i = 1; features.hasNext(); i++) {
                Feature feature = features.next();
                info("Result #" + i + ": {0}", feature);
                if (testHelper != null) {
                    testHelper.onMessage(feature);
                }
            }
        } catch (StatusRuntimeException e) {
            warning("RPC failed: {0}", e.getStatus());
            if (testHelper != null) {
                testHelper.onRpcError(e);
            }
        }
    }

    /**
     * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
     * features} with a variable delay in between. Prints the statistics when they are sent from the
     * server.
     */
    public void recordRoute(List<Feature> features, int numPoints) throws InterruptedException {
        info("*** RecordRoute");

        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<RouteSummary> routeSummaryStreamObserver = new StreamObserver<RouteSummary>() {
            @Override
            public void onNext(RouteSummary summary) {
                info("Finished trip with {0} points. Passed {1} features. "
                                + "Travelled {2} meters. It took {3} seconds.", summary.getPointCount(),
                        summary.getFeatureCount(), summary.getDistance(), summary.getElapsedTime());
            }

            @Override
            public void onError(Throwable t) {
                warning("RecordRoute Failed: {0}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RecordRoute");
                finishLatch.countDown();
            }
        };

        StreamObserver<Point> requestObserver = asyncStub.recordRoute(routeSummaryStreamObserver);

        try {
            for (int i = 0; i < numPoints; ++i) {
                int index = random.nextInt(features.size());
                Point point = features.get(index).getLocation();
                requestObserver.onNext(point);
            }
        } catch (RuntimeException ex) {
            requestObserver.onError(ex);
            throw ex;
        }
        requestObserver.onCompleted();

        // Receiving happens asynchronously
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            warning("recordRoute can not finish within 1 minutes");
        }
    }

    public CountDownLatch routeChat() {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver = new StreamObserver<RouteNote>() {
            //response
            @Override
            public void onNext(RouteNote note) {
                info("Got message \"{0}\" at {1}, {2}", note.getMessage(), note.getLocation()
                        .getLatitude(), note.getLocation().getLongitude());
            }

            @Override
            public void onError(Throwable t) {
                warning("RouteChat Failed: {0}", Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                info("Finished RouteChat");
                finishLatch.countDown();
            }
        };

        try {
            RouteNote[] requests =
                    {newNote("First message", 0, 0), newNote("Second message", 0, 1),
                            newNote("Third message", 1, 0), newNote("Fourth message", 1, 1)};

            for(RouteNote routeNote : requests) {
                requestObserver.onNext(routeNote);
            }

        } catch (RuntimeException e) {
            requestObserver.onError(e);
        }

        requestObserver.onCompleted();
        return finishLatch;
    }


    /** Issues several different requests and then exits. */
    public static void main(String[] args) throws InterruptedException {

        //Logger.getLogger("io.grpc").setLevel(Level.FINEST);

        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        final int REUESTNUM = 12;
        //RouteGuideClient client = new RouteGuideClient("192.168.21.225", 7860);
        //RouteGuideClient client1 = new RouteGuideClient("192.168.21.225", 7860);
        //CountDownLatch finishLatch = new CountDownLatch(REUESTNUM);
        //LoadBalancer loadBalancer = new LoadBalancer("192.168.1.215", 8500);
        //loadBalancer.subscribe("RouteGuideServer");

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        RouteGuideClient client = new RouteGuideClient("192.168.1.215", 8500);
        for (int i = 0; i < 1000; i++) {
            Random random = new Random();
            int index = random.nextInt(features.size());
            int lat = features.get(index).getLocation().getLatitude();
            int lon = features.get(index).getLocation().getLongitude();
            client.getFeature(lat, lon);
            //client.listFeatures(400000000, -750000000, 420000000, -730000000);
            logger.info("request location: "+ lat + ", "+  lon);
            client.shutdown();
            Thread.sleep(1000);
        }

        for (int j = 0; j < 1; j++) {
            executorService.execute(() -> {
                //RouteGuideClient client = new RouteGuideClient("192.168.1.209", 7860, loadBalancer);
                //RouteGuideClient client = new RouteGuideClient("192.168.1.215", 8500);
                CountDownLatch finishLatch = new CountDownLatch(REUESTNUM);
                long start = System.nanoTime();
                try {
                    for (int i = 0; i < REUESTNUM; i++) {
                        //int lat = getRandomNumberInRange(400146138, 419146138);
                        //int lon = -getRandomNumberInRange(740188906, 749188906);
                        //routeGuideDemo.getFeature(409146138, -746188906);
                        Random random = new Random();
                        int index = random.nextInt(features.size());
                        int lat = features.get(index).getLocation().getLatitude();
                        int lon = features.get(index).getLocation().getLongitude();
                        info("request location: {0} , {1}", lat, lon);
                        //long start = System.nanoTime();
                        client.getFeaturesAsync(lat, lon, finishLatch);
                        //client.getFeature(lat, lon);
                        //client1.getFeaturesAsync(lat, lon, finishLatch);
                        //long estimatedTime = System.nanoTime() - start;
                        //logger.info("estimatedTime: " + estimatedTime);
                    }
                    finishLatch.await(4, TimeUnit.SECONDS);
                    //client.shutdown();

                } catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    //finishLatch.await(10, TimeUnit.MINUTES);
                    long estimatedTime = System.nanoTime() - start;
                    logger.info("estimatedTime: " + estimatedTime);

                }
            });
        }







//        long start = System.nanoTime();
//        try {
//            //client.recordRoute(features, 10);
//
////            client.getFeature(409146138, -746188906);
////
////            client.listFeatures(400000000, -750000000, 420000000, -730000000);
////
////            CountDownLatch finishLatch = client.routeChat();
////
////            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
////                client.warning("routeChat can not finish within 1 minutes");
////            }
//
//
//            for (int i = 0; i < REUESTNUM; i++) {
//                //int lat = getRandomNumberInRange(400146138, 419146138);
//                //int lon = -getRandomNumberInRange(740188906, 749188906);
//                //routeGuideDemo.getFeature(409146138, -746188906);
//                Random random = new Random();
//                int index = random.nextInt(features.size());
//                int lat = features.get(index).getLocation().getLatitude();
//                int lon = features.get(index).getLocation().getLongitude();
//                info("request location: {0} , {1}", lat, lon);
//                //long start = System.nanoTime();
//                client.getFeaturesAsync(lat, lon, finishLatch);
//                client1.getFeaturesAsync(lat, lon, finishLatch);
//                //long estimatedTime = System.nanoTime() - start;
//                //logger.info("estimatedTime: " + estimatedTime);
//            }
//
//        } finally {
//            finishLatch.await(10,  TimeUnit.MINUTES);
//            long estimatedTime = System.nanoTime() - start;
//            logger.info("estimatedTime: " + estimatedTime);
//            client.shutdown();
//        }
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build()).build();
    }

    /**
     * Only used for unit test, as we do not want to introduce randomness in unit test.
     */
    @VisibleForTesting
    void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Only used for helping unit test.
     */
    @VisibleForTesting
    interface TestHelper {
        /**
         * Used for verify/inspect message received from server.
         */
        void onMessage(Message message);

        /**
         * Used for verify/inspect error received from server.
         */
        void onRpcError(Throwable exception);
    }

    @VisibleForTesting
    void setTestHelper(TestHelper testHelper) {
        this.testHelper = testHelper;
    }
}
