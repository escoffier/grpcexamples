import resilience4jhelloworld.HelloWorldDemo;

public class Client {
    public static void main(String[] argc) throws Exception {
        HelloWorldDemo helloWorldDemo = new HelloWorldDemo();
        helloWorldDemo.start();

        helloWorldDemo.testRateLimiter();
        //testRateLimiter(helloWorldDemo);
        //testCircuitBreaker(helloWorldDemo);
        //testGetname(helloWorldDemo);

    }
}
