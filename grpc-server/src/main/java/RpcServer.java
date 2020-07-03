import helloworld.GreeterService;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routeguide.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

public class RpcServer {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    //private final int port;
    private final Server server;
    //MonitoringServerInterceptor monitoringInterceptor;
    private ServiceRegister serviceRegister;
    private ServiceConfig serviceConfig;

    public RpcServer(ServiceConfig serviceConfig, RegistryConfig registryConfig, Collection<BindableService> services) throws IOException {
        this.serviceConfig = serviceConfig;
        this.serviceRegister = new ZKServiceRegister(registryConfig);
        ServerBuilder<?> builder = ServerBuilder.forPort(serviceConfig.getPort());
        for (BindableService service : services) {
            builder.addService(service);
        }

        builder.addService(new HealthCheckService());

        server = builder
                .executor(Executors.newFixedThreadPool(4))
                .build();
    }

    public void start() throws Exception {
        server.start();
        serviceRegister.registerService(serviceConfig);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            logger.info("need paramater");
            return;
        }

        logger.info(args[0]);
        Properties prop = new Properties();
        InputStream inputStream = new FileInputStream(args[0]);
        prop.load(inputStream);

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setHost(prop.getProperty("server.host"));
        serviceConfig.setPort(Integer.parseInt(prop.getProperty("server.port")));
        serviceConfig.setServiceName("RouteGuideService");
        serviceConfig.setServiceDescription("test");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setHost(prop.getProperty("registry.host"));
        registryConfig.setPort(Integer.parseInt(prop.getProperty("registry.port")));

        List<BindableService> serviceList = new ArrayList<>();
        serviceList.add(new RouteGuideService(RouteGuideUtil.getDefaultFeaturesFile()));
        serviceList.add(new GreeterService());

        RpcServer server = new RpcServer(serviceConfig, registryConfig, serviceList);

        server.start();
        server.blockUntilShutdown();
//        ServiceConfig serviceConfig = new ServiceConfig();
//        serviceConfig.setHost(prop.getProperty("server.host"));
//        serviceConfig.setPort(Integer.parseInt(prop.getProperty("server.port")));
//        serviceConfig.setServiceName("RouteGuideService");
//        serviceConfig.setServiceDescription("test");
//
//
//        RouteGuideServer routeGuideServer = new RouteGuideServer(serviceConfig);
//
//        RegistryConfig registryConfig = new RegistryConfig();
//        registryConfig.setHost(prop.getProperty("registry.host"));
//        registryConfig.setPort(Integer.parseInt(prop.getProperty("registry.port")));
//        routeGuideServer.init(registryConfig);
//        routeGuideServer.start();
//        routeGuideServer.blockUntilShutdown();
    }
}
