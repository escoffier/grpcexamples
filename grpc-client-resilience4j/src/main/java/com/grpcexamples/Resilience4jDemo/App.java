package com.grpcexamples.Resilience4jDemo;

public class App {
    public static void main(String[] args) {

        HelloWorldDemo helloWorldDemo = new HelloWorldDemo();
        helloWorldDemo.start();
        helloWorldDemo.testRateLimiter();

        //testRateLimiter(helloWorldDemo);
        //testCircuitBreaker(helloWorldDemo);
        //helloWorldDemo.testGetname( );
    }
}
