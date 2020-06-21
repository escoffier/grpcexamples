package routeguide;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
//import java.util.logging.Logger;


public class RouteGuideServer {
    private static final Logger logger = LoggerFactory.getLogger(RouteGuideServer.class.getName());

    private final int port;
    private final Server server;
    MonitoringServerInterceptor monitoringInterceptor;
    private ServiceRegister serviceRegister;
    private ServiceConfig serviceConfig;

    public RouteGuideServer(ServiceConfig serviceConfig) throws IOException {
        this(serviceConfig.getPort(), RouteGuideUtil.getDefaultFeaturesFile());
        this.serviceConfig = serviceConfig;
    }

    public RouteGuideServer(int port) throws IOException {
        this(port, RouteGuideUtil.getDefaultFeaturesFile());
    }

    public RouteGuideServer(int port, URL featureFile) throws IOException {
        this(ServerBuilder.forPort(port), port, RouteGuideUtil.parseFeatures(featureFile));
    }

    public RouteGuideServer(ServerBuilder<?> serverBuilder, int port, Collection<Feature> features) {
        this.port = port;
        monitoringInterceptor = MonitoringServerInterceptor.create(Configuration.cheapMetricsOnly());
        //server = serverBuilder.addService(new RouteGuideService(features)).build();
        server = serverBuilder.addService(ServerInterceptors.intercept( new RouteGuideService(features), monitoringInterceptor))
                .addService(new HealthCheckService())
                .executor(Executors.newFixedThreadPool(4))
                .build();
        //agentClient.pass(serviceId);
    }

    public void init(RegistryConfig registryConfig) {
        try {
            logger.info("connecting consul " + registryConfig.getHost() + ":"+ port);
//            StringBuffer stringBuffer = new StringBuffer(registryConfig.getHost());
//            stringBuffer.append(":" + registryConfig.getPort());

            serviceRegister = new ZKServiceRegister(registryConfig);
                    //serverHost, serverPort, "RouteGuideService", "test");

//            ServiceConfig config = new ServiceConfig();
//            config.setHost(host);
//            config.setPort(port);
//            config.setServiceName("RouteGuideService");
//            config.setServiceDescription("test");
            serviceRegister.registerService(serviceConfig);
//            Consul client =  Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port))
//                    .build();
//            AgentClient agentClient = client.agentClient();
//            String serviceId = "RouteGuideServer-" + id; //+ random.nextInt();
//            Registration service = ImmutableRegistration.builder()
//                    .id(serviceId)
//                    .name("RouteGuideServer")
//                    .address(serverHost)
//                    .port(serverPort)
//                    .tags(Collections.singletonList("tag1"))
//                    .meta(Collections.singletonMap("version", "1.0"))
//                    .check(Registration.RegCheck.grpc(new StringBuilder(serverHost).append(":").append(serverPort).toString(), 10))
//                    .build();
//
//            agentClient.register(service);
        } catch (Exception ex) {
            logger.info(ex.getMessage());
        }
    }


    public void start() throws IOException {
        server.start();
        logger.info("RouteGuideServer started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RouteGuideServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

//    public static void main(String[] args) throws Exception {
//        if(args.length == 0) {
//            logger.info("need paramater");
//            return;
//        }
//
//        logger.info(args[0]);
//        Properties prop = new Properties();
//        InputStream inputStream = new FileInputStream(args[0]);
//        prop.load(inputStream);
//        String serverHost = prop.getProperty("server.host");
//        int serverPort = Integer.parseInt(prop.getProperty("server.port"));
//
//        RouteGuideServer routeGuideServer = new RouteGuideServer(serverPort);
//
//        ServiceConfig config = new ServiceConfig();
//        config.setHost(prop.getProperty("consul.host"));
//        config.setPort(Integer.parseInt(prop.getProperty("consul.port")));
//        config.setServiceName("RouteGuideService");
//        config.setServiceDescription("test");
//
//        routeGuideServer.init(config);
//
////        routeGuideServer.init(prop.getProperty("consul.host"), Integer.parseInt(prop.getProperty("consul.port")),
////                serverHost, serverPort,
////                Integer.parseInt(prop.getProperty("service.id")));
//        routeGuideServer.start();
//        routeGuideServer.blockUntilShutdown();
//    }

}
