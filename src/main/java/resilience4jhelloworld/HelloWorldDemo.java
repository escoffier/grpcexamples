package resilience4jhelloworld;

import helloworld.HelloReply;
import helloworld.HelloWorldClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;

import java.time.Duration;
import java.util.logging.Logger;

public class HelloWorldDemo {

    private static Logger logger = Logger.getLogger(HelloWorldDemo.class.getName());
    private HelloWorldClient helloWorldClient;
    private CircuitBreaker circuitBreaker;

    public static void main(String[] argc) {

        HelloWorldDemo helloWorldDemo = new HelloWorldDemo();
        helloWorldDemo.sayHello();
    }

    public HelloWorldDemo() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        circuitBreaker = registry.circuitBreaker("helloworld");

        helloWorldClient = new HelloWorldClient("localhost", 50051);
    }

    public void sayHello() {
        CheckedFunction0<HelloReply> decoratedSupplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () -> {
            HelloReply helloReply = helloWorldClient.greeting("robbies");
            return helloReply;
        });

        Try<HelloReply> result = Try.of(decoratedSupplier);

        logger.info(result.get().getMessage());
    }
}
