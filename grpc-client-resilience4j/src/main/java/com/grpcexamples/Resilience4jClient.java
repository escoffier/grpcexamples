package com.grpcexamples;

import helloworld.GreeterGrpc;
import helloworld.HelloReply;
import helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Resilience4jClient {

    private static Logger logger = LoggerFactory.getLogger(Resilience4jClient.class);

    private final ManagedChannel channel;

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    private final GreeterGrpc.GreeterStub asyncStub;

    private final GreeterGrpc.GreeterFutureStub futureStub;

    public Resilience4jClient(String host, int port){
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    public Resilience4jClient(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.asyncStub = GreeterGrpc.newStub(channel);
        this.futureStub = GreeterGrpc.newFutureStub(channel);
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
