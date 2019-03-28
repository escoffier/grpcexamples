package resilience4jhelloworld;

import helloworld.HelloReply;
import helloworld.HelloWorldClient;
import helloworld.NameMessage;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class HelloWorldDemo {

    private static Logger logger = Logger.getLogger(HelloWorldDemo.class.getName());
    private static HelloWorldClient helloWorldClient;
    private CircuitBreaker circuitBreaker;
    private RateLimiter rateLimiter;
    private Bulkhead bulkhead;

    public static void main(String[] argc) throws Exception {
        HelloWorldDemo helloWorldDemo = new HelloWorldDemo();
        HelloWorldDemo.start();

        //testRateLimiter(helloWorldDemo);
        //testCircuitBreaker(helloWorldDemo);
        testGetname(helloWorldDemo);

    }

    public static void testCircuitBreaker(HelloWorldDemo helloWorldDemo) {
        CircuitBreaker.Metrics metrics = helloWorldDemo.getCircuitBreaker().getMetrics();
        for (int i = 0 ; i < 40; i++) {


            //final CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                helloWorldDemo.sayHello();
                //helloWorldDemo.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                float failRate = metrics.getFailureRate();
                logger.info("------------fail rate: " + failRate);
                //helloWorldDemo.shutdown();
            }
        }
    }

    public static void testRateLimiter(HelloWorldDemo helloWorldDemo) {
        RateLimiter.Metrics metrics = helloWorldDemo.getRateLimiter().getMetrics();
        for (int i = 0 ; i < 40; i++) {
            try {
                helloWorldDemo.rlSayHello();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                int availablePermissions = metrics.getAvailablePermissions();
                logger.info("------------rate limiter availablePermissions: " + availablePermissions);
            }
        }
    }

    public static void testGetname(HelloWorldDemo helloWorldDemo) {
        String name = helloWorldDemo.getName("122222");
        logger.info("name is: " + name);
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public static void start() {
        helloWorldClient = new HelloWorldClient("140.143.45.252", 50051);
        //helloWorldClient = new HelloWorldClient("127.0.0.1", 50051);
    }

    public HelloWorldDemo() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofMillis(10000))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(10)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("helloworld");

        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(1000))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofMillis(25))
                .build();

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
        rateLimiter = rateLimiterRegistry.rateLimiter("helloworld ratelimiter");

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(100)
                .maxWaitTime(100)
                .build();

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfig);

        bulkhead = bulkheadRegistry.bulkhead("get name bulkhead");
        bulkhead.getEventPublisher()
                .onCallPermitted(event -> {logger.info(event.getBulkheadName());});
    }

    public static void shutdown() throws InterruptedException {
        helloWorldClient.shutdown();
    }

    public void rlSayHello() {
        CheckedFunction0<HelloReply> replyCheckedFunction0 = RateLimiter.decorateCheckedSupplier(rateLimiter, () ->{
            //            if(Math.random() > 0.60) {
//                throw new RuntimeException("Simulated failure");
//            }

            HelloReply helloReply = helloWorldClient.greeting("robbie for rate limiter");
            return helloReply;
        });

        Try<HelloReply> result = Try.of(replyCheckedFunction0);

        logger.info(result.get().getMessage());
    }

    public String getName(String id) {
        CheckedFunction0<String> replyCheckedFunction0 = Bulkhead.decorateCheckedSupplier(bulkhead, ()->{
            NameMessage nameMessage = helloWorldClient.getName(id);
            return nameMessage.getName();
        });

        Try<String> result = Try.of(replyCheckedFunction0);
        return result.get();
    }

    public void sayHello() {
        CheckedFunction0<HelloReply> decoratedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> {
            if(Math.random() > 0.60) {
                throw new RuntimeException("Simulated failure");
            }

            HelloReply helloReply = helloWorldClient.greeting("robbies");
            return helloReply;
        });

        CheckedFunction0<HelloReply> replyCheckedFunction0 =   RateLimiter.decorateCheckedSupplier(rateLimiter, decoratedSupplier);

        Try<HelloReply> result = Try.of(replyCheckedFunction0);

        logger.info(result.get().getMessage());
    }
}
