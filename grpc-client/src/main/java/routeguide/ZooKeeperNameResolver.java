package routeguide;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZooKeeperNameResolver extends NameResolver {
    private Logger logger = LoggerFactory.getLogger(ZooKeeperNameResolver.class);

    CuratorFramework client;
    ServiceCache serviceCache;
    ServiceDiscovery<InstanceDetails> serviceDiscovery;
    private static final String     PATH = "/discovery/services/";
    private static final String     SERVICE_NAME = "RouteGuideService";
    private final URI uri;

    ZooKeeperNameResolver(URI uri) {
        this.uri = uri;
        StringBuilder connectString = new StringBuilder(uri.getHost());
        connectString.append(":").append(uri.getPort());
        client = CuratorFrameworkFactory.newClient(connectString.toString(),
                new ExponentialBackoffRetry(1000, 3));
        client.start();

        JsonInstanceSerializer<InstanceDetails> serializer = new JsonInstanceSerializer<InstanceDetails>(InstanceDetails.class);
        serviceDiscovery = ServiceDiscoveryBuilder.builder(InstanceDetails.class)
                .client(client)
                .basePath(PATH)
                .serializer(serializer)
                .build();
        try {
            serviceDiscovery.start();
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
        }
    }

    @Override
    public String getServiceAuthority() {
        return this.uri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        try {
            Collection<ServiceInstance<InstanceDetails>> serviceInstances =  serviceDiscovery.queryForInstances(SERVICE_NAME);
            loadServiceNodes(listener, serviceInstances);
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
        }

        //loadServiceNodes(listener);
        serviceCache = serviceDiscovery.serviceCacheBuilder()
                .name(SERVICE_NAME)
                .build();
        serviceCache.addListener(new ServiceCacheListener() {
            @Override
            public void cacheChanged() {
                List<ServiceInstance<InstanceDetails>> serviceInstances = serviceCache.getInstances();
                logger.info("########### "+ serviceInstances.toString());
                loadServiceNodes(listener, serviceInstances);
            }

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                logger.info(newState.toString());
            }
        });

        try {
            serviceCache.start();
        } catch ( Exception ex) {
            logger.warn(ex.getMessage());
        }
    }

    @Override
    public void shutdown() {

    }

    private void loadServiceNodes(Listener listener,  Collection<ServiceInstance<InstanceDetails>> serviceInstances) {
        List<EquivalentAddressGroup> addressGroups = new ArrayList<>();
        for (ServiceInstance<InstanceDetails> serviceInstance : serviceInstances) {
            logger.info("################ service address( " + serviceInstance.getAddress()+ ":" + serviceInstance.getPort() + " )");
            List<SocketAddress> sockAddrsList = new ArrayList<>();
            sockAddrsList.add(new InetSocketAddress(serviceInstance.getAddress(), serviceInstance.getPort()));
            addressGroups.add(new EquivalentAddressGroup(sockAddrsList));
        }
        listener.onAddresses(addressGroups, Attributes.EMPTY);
    }
    private void loadServiceNodes(Listener listener) {
//        List<EquivalentAddressGroup> addressGroups = new ArrayList<>();
//        try {
//            Collection<ServiceInstance<InstanceDetails>> serviceInstances =  serviceDiscovery.queryForInstances(SERVICE_NAME);
//
//            for (ServiceInstance<InstanceDetails> serviceInstance : serviceInstances) {
//                logger.info("################ service address( " + serviceInstance.getAddress()+ ":" + serviceInstance.getPort() + " )");
//                List<SocketAddress> sockAddrsList = new ArrayList<>();
//                sockAddrsList.add(new InetSocketAddress(serviceInstance.getAddress(), serviceInstance.getPort()));
//                addressGroups.add(new EquivalentAddressGroup(sockAddrsList));
//            }
//        } catch (Exception ex) {
//            logger.warn(ex.getMessage());
//        } finally {
//            listener.onAddresses(addressGroups, Attributes.EMPTY);
//        }
    }

    public static class ZooKeeperNameResolverProvider extends NameResolverProvider {
        private String serviceName;

        public ZooKeeperNameResolverProvider(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 0;
        }

        @Override
        public String getDefaultScheme() {
            return "zookeeper";
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, Helper helper) {
            return new ZooKeeperNameResolver(targetUri);
        }

//        @Override
//        public NameResolver newNameResolver(URI targetUri, Args args) {
////            return super.newNameResolver(targetUri, args);
//            return new ZooKeeperNameResolver(targetUri);
//        }
    }
}
