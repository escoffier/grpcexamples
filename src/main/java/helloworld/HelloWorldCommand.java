package helloworld;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HelloWorldCommand extends HystrixCommand<HelloReply> {

    private static final Logger logger = Logger.getLogger(HelloWorldCommand.class.getName());
    private static HelloWorldClient helloWorldClient;

    public HelloWorldCommand() {
        //super(setter);
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("HelloWorld"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withCircuitBreakerForceOpen(true))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(10)));

        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", 8);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.HelloWorldCommand.execution.isolation.thread.timeoutInMilliseconds", 3000);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.HystrixCommandKey.circuitBreaker.forceOpen", true);


        helloWorldClient = new HelloWorldClient("localhost", 50051);
    }

    public void stop() throws InterruptedException {
        helloWorldClient.shutdown();
    }

    @Override
    protected HelloReply getFallback() {
        return HelloReply.newBuilder().setMessage("nothing").build();
    }

    @Override
    protected HelloReply run() throws Exception {
        HelloReply helloReply = helloWorldClient.greeting("robbies");
        return helloReply;
    }

    public static void startMetricsMonitor() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(HelloWorldCommand.class.getSimpleName()));

                    StringBuilder out = new StringBuilder();
                    out.append("\n");
                    out.append("#####################################################################################").append("\n");
                    out.append("# HelloWorldCommand: " + getStatsStringFromMetrics(metrics)).append("\n");
                    out.append("#####################################################################################").append("\n");
                    System.out.println(out.toString());
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private static String getStatsStringFromMetrics(HystrixCommandMetrics metrics) {
        StringBuilder m = new StringBuilder();
        if (metrics != null) {
            HystrixCommandMetrics.HealthCounts health = metrics.getHealthCounts();
            m.append("Requests: ").append(health.getTotalRequests()).append(" ");
            m.append("Errors: ").append(health.getErrorCount()).append(" (").append(health.getErrorPercentage()).append("%)   ");
            m.append("Mean: ").append(metrics.getExecutionTimePercentile(50)).append(" ");
            m.append("75th: ").append(metrics.getExecutionTimePercentile(75)).append(" ");
            m.append("90th: ").append(metrics.getExecutionTimePercentile(90)).append(" ");
            m.append("99th: ").append(metrics.getExecutionTimePercentile(99)).append(" ");
        }
        return m.toString();
    }



    public static void main(String [] args) throws Exception {
        final HystrixRequestContext context = HystrixRequestContext.initializeContext();
        //HelloReply helloReply = new HelloWorldCommand().execute();

        startMetricsMonitor();
        while (true) {
            final CountDownLatch latch = new CountDownLatch(1);
            HelloWorldCommand helloWorldCommand = new HelloWorldCommand();

            Observable<HelloReply> replyObservable = helloWorldCommand.observe();

            replyObservable.subscribe(new Subscriber<HelloReply>() {
                @Override
                public void onCompleted() {

                    //logger.info("onCompleted");
                    latch.countDown();
                    context.shutdown();
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    context.shutdown();
                    latch.countDown();
                }

                @Override
                public void onNext(HelloReply helloReply) {
                    //logger.info(Thread.currentThread().getName());
                    logger.info(helloReply.getMessage());
                }
            });

            latch.await(5000, TimeUnit.MILLISECONDS);
            helloWorldCommand.stop();
            //HelloReply helloReply = new HelloWorldCommand().execute();
            //logger.info(helloReply.getMessage());
        }

    }
}
