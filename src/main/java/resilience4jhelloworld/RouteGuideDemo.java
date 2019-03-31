package resilience4jhelloworld;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import routeguide.*;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RouteGuideDemo {
    //private static RouteGuideClient routeGuideClient;
    private static Logger logger = Logger.getLogger(RouteGuideDemo.class.getName());
    private Bulkhead bulkhead;

    private  static List<Feature> features;

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;

    RouteGuideDemo(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());

        //routeGuideClient = new RouteGuideClient("192", 7860);

    }

    private static void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    RouteGuideDemo(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();

        blockingStub = RouteGuideGrpc.newBlockingStub(channel);

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(100)
                .maxWaitTime(100)
                .build();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfig);

        bulkhead = bulkheadRegistry.bulkhead("RouteGuide");
    }

    public static void init() throws Exception {
        URL featureFile = RouteGuideUtil.getDefaultFeaturesFile();
        features = RouteGuideUtil.parseFeatures(featureFile);
    }

    public void getFeature(int lat, int lon) {
        //;
        CheckedFunction0<Feature> function0 =  Bulkhead.decorateCheckedSupplier(bulkhead, () -> {
            info("getFeature location: {0} {1}", lat, lon);
            Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
            Feature feature = blockingStub.getFeature(request);
            return feature;
        });

        Try<Feature> featureTry = Try.of(function0);

        Feature feature = featureTry.get();

        if (RouteGuideUtil.exists(feature)) {

            info("Found a feature called \"{0}\" at {1}, {2}",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
            info("Found no feature at {0}, {1}",
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        }

//        Bulkhead.decorateRunnable(bulkhead, ()->{
//            routeGuideClient.getFeature();
//        });

    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static void main(String[] args) throws Exception {
        RouteGuideDemo.init();
        RouteGuideDemo routeGuideDemo = new RouteGuideDemo("140.143.45.252", 7860);

        //int lat = (int) 19146138 * Math.random();

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        for (int j = 0; j < 4; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 20; i++) {
                    //int lat = getRandomNumberInRange(400146138, 419146138);
                    //int lon = -getRandomNumberInRange(740188906, 749188906);
                    //routeGuideDemo.getFeature(409146138, -746188906);
                    Random r = new Random();
                    int index = r.nextInt(features.size());
                    int lat = features.get(index).getLocation().getLatitude();
                    int lon = features.get(index).getLocation().getLongitude();
                    info("request location: {0} , {1}", lat, lon);
                    long start = System.nanoTime();
                    routeGuideDemo.getFeature(lat, lon);
                    long estimatedTime = System.nanoTime() - start;
                    logger.info("estimatedTime: " + estimatedTime);
                }
            });
        }

//        for (int i = 0; i < 10; i++) {
//            //int lat = getRandomNumberInRange(400146138, 419146138);
//            //int lon = -getRandomNumberInRange(740188906, 749188906);
//            //routeGuideDemo.getFeature(409146138, -746188906);
//            Random r = new Random();
//            int index = r.nextInt(features.size());
//            int lat = features.get(index).getLocation().getLatitude();
//            int lon = features.get(index).getLocation().getLongitude();
//            info("request location: {0} , {1}", lat, lon );
//            long start = System.nanoTime();
//            routeGuideDemo.getFeature(lat, lon);
//            long estimatedTime = System.nanoTime() - start;
//            logger.info("estimatedTime: "  + estimatedTime);
//        }

    }
}
