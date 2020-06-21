package com.grpcexamples;

import com.alibaba.csp.sentinel.adapter.grpc.SentinelGrpcClientInterceptor;
import helloworld.GreeterGrpc;
import helloworld.HelloReply;
import helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public class ServiceClient {
    private static   Logger logger = LoggerFactory.getLogger(ServiceClient.class);
    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    ServiceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .intercept(new SentinelGrpcClientInterceptor()) // Add the client interceptor.
                .build();
        // Init your stub here.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response = null;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.warn( "------greet RPC failed: {0}", e.getStatus());
        } finally {
            if (response != null) {
                logger.info("Greeting: " + response.getMessage());
            }
        }
    }
}
