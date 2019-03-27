package resilience4jhelloworld;

import helloworld.HelloReply;
import helloworld.HelloWorldClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class HelloWorldDemo {

    private static Logger logger = Logger.getLogger(HelloWorldDemo.class.getName());
    private static HelloWorldClient helloWorldClient;
    private CircuitBreaker circuitBreaker;

    public static void main(String[] argc) throws Exception {
        HelloWorldDemo helloWorldDemo = new HelloWorldDemo();
        HelloWorldDemo.start();

        for (int i = 0 ; i < 20; i++) {


            //final CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                helloWorldDemo.sayHello();
                //helloWorldDemo.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CircuitBreaker.Metrics metrics = helloWorldDemo.getCircuitBreaker().getMetrics();
                float failRate = metrics.getFailureRate();

                logger.info("------------fail rate: " + failRate);
                //helloWorldDemo.shutdown();
            }
        }


    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public static void start() {
        helloWorldClient = new HelloWorldClient("140.143.45.252", 50051);
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
    }

    public static void shutdown() throws InterruptedException {
        helloWorldClient.shutdown();
    }

    public void sayHello() {
        CheckedFunction0<HelloReply> decoratedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> {
            if(Math.random() > 0.60) {
                throw new RuntimeException("Simulated failure");
            }

            HelloReply helloReply = helloWorldClient.greeting("robbies");
            return helloReply;
        });

        Try<HelloReply> result = Try.of(decoratedSupplier);

        logger.info(result.get().getMessage());
    }
}
