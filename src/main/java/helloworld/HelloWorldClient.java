package helloworld;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HelloWorldClient {

    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final ManagedChannel channel;

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    private final GreeterGrpc.GreeterStub asyncStub;

    private final GreeterGrpc.GreeterFutureStub futureStub;

    public HelloWorldClient(String host, int port){
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    public HelloWorldClient(ManagedChannel channel) {

        this.channel = channel;
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        this.asyncStub = GreeterGrpc.newStub(channel);
        this.futureStub = GreeterGrpc.newFutureStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    public void futureDirectGreet(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();

        ListenableFuture<HelloReply> response = futureStub.sayHello(request);
        try {
            HelloReply reply = response.get();

            logger.info("futureDirectGreet: " + reply.toString());
        } catch (InterruptedException e) {
            logger.info(e.getMessage());

        } catch (ExecutionException e) {
            logger.info(e.getMessage());
        }


    }

    public void asyncGreet(String name) {

        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        final CountDownLatch latch = new CountDownLatch(1);

        asyncStub.sayHello(request, new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply value) {
                logger.info("asyncGreet: " + value.toString());
            }

            @Override
            public void onError(Throwable t) {

                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

//        if (!latch.await(1, TimeUnit.MINUTES)) {
//            client.warning("routeChat can not finish within 1 minutes");
//        }
        if (!Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.SECONDS)) {
            throw new RuntimeException("timeout!");
        }
    }


    public static void main(String [] args) throws Exception {
        HelloWorldClient client = new HelloWorldClient("localhost", 50051);

        try {
            String user = "robbie";
            if (args.length > 0) {
                user = args[0]; /* Use the arg as the name to greet if provided */
            }
            client.greet(user);

            client.asyncGreet("qunfeng");

            client.futureDirectGreet("qunfengqiu");
        } finally {
            client.shutdown();
        }
    }

}
