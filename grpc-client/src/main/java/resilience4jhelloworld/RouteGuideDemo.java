package resilience4jhelloworld;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverProvider;
import io.grpc.stub.StreamObserver;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routeguide.*;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
//import java.util.logging.Logger;

public class RouteGuideDemo {
    //private static RouteGuideClient routeGuideClient;
    private static Logger logger = LoggerFactory.getLogger(RouteGuideDemo.class);
    private Bulkhead bulkhead;

    private  static List<Feature> features;

    private final ManagedChannel channel;
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;
    private  RouteGuideGrpc.RouteGuideStub asyncStub;

    private RateLimiter rateLimiter;

//    RouteGuideDemo(String host, int port) {
//        this(ManagedChannelBuilder
//                //.forAddress(host, port)
//                .forTarget("consul://"+ host+ ":" + port)
//                .defaultLoadBalancingPolicy("round_robin")
//                .nameResolverFactory(new ConsulNameResolver.ConsulNameResolverProvider("RouteGuideServer", null))
//                .usePlaintext());
//    }

    RouteGuideDemo(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();

        blockingStub = RouteGuideGrpc.newBlockingStub(channel);

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(10_000)
                .maxWaitTime(100)
                .build();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfig);

        bulkhead = bulkheadRegistry.bulkhead("RouteGuide");
    }

    public RouteGuideDemo(String host, int port, NameResolverProvider nameResolverProvider) {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
                .forTarget( "zookeeper://" + host+ ":" + port)
                .intercept(new HeaderClientInterceptor())
                .defaultLoadBalancingPolicy("round_robin")
                .nameResolverFactory(nameResolverProvider)
                .usePlaintext();

        channel = channelBuilder.build();
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);

        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(100))
                .limitForPeriod(5000)
                .timeoutDuration(Duration.ofMillis(25))
                .build();

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
        rateLimiter = rateLimiterRegistry.rateLimiter("RouteGuide Ratelimiter");
    }

    public static void init() throws Exception {
        URL featureFile = RouteGuideUtil.getDefaultFeaturesFile();
        features = RouteGuideUtil.parseFeatures(featureFile);
    }

    public void getFeature(int lat, int lon) {
        CheckedFunction0<Feature> function0 =  Bulkhead.decorateCheckedSupplier(bulkhead, () -> {
            logger.info("getFeature location: {} {}", lat, lon);
            Point request = Point.newBuilder()
                    .setLatitude(lat)
                    .setLongitude(lon)
                    .build();

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
    }

    public void getFeatureRateLimiter(int lat, int lon) {

        Supplier<Feature> supplier = RateLimiter.decorateSupplier(rateLimiter,
                () -> {
                    Point point = Point.newBuilder()
                            .setLatitude(lat)
                            .setLongitude(lon)
                            .build();
                    return blockingStub.getFeature(point);
                });

        Try<Feature> featureTry = Try.ofSupplier(supplier);

        if (featureTry.isSuccess()) {
            logger.info(featureTry.get().getName());
        }

//        CheckedFunction1<Point, Feature> checkedFunction1 = RateLimiter.decorateCheckedFunction(rateLimiter,
//                blockingStub::getFeature
//        );
//
//        Point point = Point.newBuilder()
//                .setLatitude(lat)
//                .setLongitude(lon)
//                .build();

//        Try<Feature> featureTry = Try.of(() ->{
//            try {
//                return checkedFunction1.apply(point);
//            } catch (Exception ex) {
//                throw new Exception(ex);
//            }
//        });

//        logger.info(featureTry.get().getName());
    }

    public void getFeatureAsyncRateLimiter(int lat, int lon, CountDownLatch countDownLatch) {
        Runnable runnable = RateLimiter.decorateRunnable(rateLimiter,
                () -> {
                    Point point = Point.newBuilder()
                            .setLatitude(lat)
                            .setLongitude(lon)
                            .build();
                     asyncStub.getFeature(point, new StreamObserver<Feature>() {
                         @Override
                         public void onNext(Feature value) {
                             countDownLatch.countDown();
                             logger.info(value.getName() + "-------" + countDownLatch.getCount());
                         }
                         @Override
                         public void onError(Throwable t) {
                             logger.error(t.getMessage());
                             countDownLatch.countDown();
                         }

                         @Override
                         public void onCompleted() {
                             //logger.info("getFeatureAsyncRateLimiter onCompleted");
                         }
                     });
                });
        Try<Void> voidTry =  Try.runRunnable(runnable);
        if (voidTry.isFailure()) {
            logger.error(voidTry.getCause().getMessage());
        }
    }
    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static void testSync(RouteGuideDemo client, List<Feature> features) {
        Random random = new Random();
        long start = System.currentTimeMillis();
        for (int j = 0; j < 60000; j++) {
            int index = random.nextInt(features.size());
            int lat = features.get(index).getLocation().getLatitude();
            int lon = features.get(index).getLocation().getLongitude();
            client.getFeatureRateLimiter(lat, lon);
        }
        long estimatedTime = System.currentTimeMillis() - start;
        logger.info("estimatedTime: " + estimatedTime);
    }

    public static void testAsync(RouteGuideDemo client, List<Feature> features) {
        int number = 60000;
        CountDownLatch countDownLatch = new CountDownLatch(number);
        Random random = new Random();
        long start = System.currentTimeMillis();
        for (int j = 0; j < number; j++) {
            int index = random.nextInt(features.size());
            int lat = features.get(index).getLocation().getLatitude();
            int lon = features.get(index).getLocation().getLongitude();
            client.getFeatureAsyncRateLimiter(lat, lon , countDownLatch);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException ex) {
            logger.warn(ex.getMessage());
        }
        long estimatedTime = System.currentTimeMillis() - start;
        logger.info("Async estimatedTime: " + estimatedTime);
    }
    public static void main(String[] args) throws Exception {
        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        NameResolverProvider nameResolverProvider = new ZooKeeperNameResolver.ZooKeeperNameResolverProvider("RouteGuideService");
        RouteGuideDemo client = new RouteGuideDemo("192.168.254.131", 2181, nameResolverProvider);
        //testAsync(client, features);
        testSync(client, features);



//        RouteGuideDemo.init();
//        RouteGuideDemo routeGuideDemo = new RouteGuideDemo("192.168.21.248", 8500);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(4);
//
//        for (int j = 0; j < 4; j++) {
//            executorService.execute(() -> {
//                for (int i = 0; i < 20; i++) {
//                    //int lat = getRandomNumberInRange(400146138, 419146138);
//                    //int lon = -getRandomNumberInRange(740188906, 749188906);
//                    //routeGuideDemo.getFeature(409146138, -746188906);
//                    Random r = new Random();
//                    int index = r.nextInt(features.size());
//                    int lat = features.get(index).getLocation().getLatitude();
//                    int lon = features.get(index).getLocation().getLongitude();
//                    logger.info("request location: {} , {}", lat, lon);
//                    long start = System.nanoTime();
//                    routeGuideDemo.getFeature(lat, lon);
//                    long estimatedTime = System.nanoTime() - start;
//                    logger.info("estimatedTime: " + estimatedTime);
//                }
//            });
//        }

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
