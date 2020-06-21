package routeguide;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

public class ZKServiceRegister  implements ServiceRegister{
    CuratorFramework client;
    private ServiceDiscovery<InstanceDetails> serviceDiscovery;
    private  ServiceInstance<InstanceDetails> serviceInstance;
    private String serviceName;
    private final String BASE_PATH = "/discovery/services/";

    public ZKServiceRegister(String connectString) throws Exception{
        client = CuratorFrameworkFactory.newClient(connectString,
                new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public ZKServiceRegister(RegistryConfig registryConfig) {
        StringBuffer connectString = new StringBuffer(registryConfig.getHost());
        connectString.append(":" + registryConfig.getPort());

        client = CuratorFrameworkFactory.newClient(connectString.toString(),
                new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @Override
    public void registerService(ServiceConfig config) throws Exception{
        this.serviceName = config.getServiceName();

        serviceInstance = ServiceInstance.<InstanceDetails>builder()
                .name(serviceName)
                .payload(new InstanceDetails(config.getServiceDescription()))
                .address(config.getHost())
                .port(config.getPort())
                .build();

        JsonInstanceSerializer<InstanceDetails> serializer = new JsonInstanceSerializer<InstanceDetails>(InstanceDetails.class);
        serviceDiscovery = ServiceDiscoveryBuilder.builder(InstanceDetails.class)
                .client(client)
                .basePath(BASE_PATH)
                .serializer(serializer)
                .thisInstance(serviceInstance)
                .build();
        serviceDiscovery.start();
    }

    @Override
    public void unregisterService() {
        try {
            serviceDiscovery.unregisterService(serviceInstance);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
