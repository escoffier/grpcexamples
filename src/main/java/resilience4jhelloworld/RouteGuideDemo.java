package resilience4jhelloworld;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routeguide.*;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.logging.Logger;

public class RouteGuideDemo {
    //private static RouteGuideClient routeGuideClient;
    private static Logger logger = LoggerFactory.getLogger(RouteGuideDemo.class.getName());
    private Bulkhead bulkhead;

    private  static List<Feature> features;

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;

    RouteGuideDemo(String host, int port) {
        this(ManagedChannelBuilder
                //.forAddress(host, port)
                .forTarget("consul://"+ host+ ":" + port)
                .defaultLoadBalancingPolicy("round_robin")
                .nameResolverFactory(new ConsulNameResolver.ConsulNameResolverProvider("RouteGuideServer", null))
                .usePlaintext());
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
            logger.info("getFeature location: {} {}", lat, lon);
            Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();
            Feature feature = blockingStub.getFeature(request);
            return feature;
        });

        Try<Feature> featureTry = Try.of(function0);

        Feature feature = featureTry.get();

        if (RouteGuideUtil.exists(feature)) {

            logger.info("Found a feature called \"{}\" at {}, {}",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(feature.getLocation()),
                    RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
            logger.info("Found no feature at {}, {}",
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
        RouteGuideDemo routeGuideDemo = new RouteGuideDemo("192.168.21.248", 8500);

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
                    logger.info("request location: {} , {}", lat, lon);
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
