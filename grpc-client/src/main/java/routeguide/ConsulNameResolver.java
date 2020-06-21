package routeguide;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.cache.ServiceHealthCache;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsulNameResolver extends NameResolver {
    private URI uri;
    private String serviceName;
//    private int pauseInSeconds;
//    private boolean ignoreConsul;
//    private List<String> hostPorts;
    private List<Service> serviceList;

    private Logger logger = LoggerFactory.getLogger(ConsulNameResolver.class);

    ServiceHealthCache serviceHealthCache;
    private Consul consul;

    public ConsulNameResolver(URI uri, String serviceName) {
        this.uri = uri;
        this.serviceName = serviceName;
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(uri.getHost(), uri.getPort())).build();
    }

    private Listener listener;

    @Override
    public String getServiceAuthority() {
        return this.uri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        //serviceHealthCache = ServiceHealthCache.newCache(consul.healthClient(), serviceName);
        serviceHealthCache = ServiceHealthCache
                .newCache(consul.healthClient(), serviceName);
        loadServiceNodes();
        subscribe(serviceName);
    }

    @Override
    public void shutdown() {
        serviceHealthCache.stop();
    }

    private void loadServiceNodes() {
//        List<SocketAddress> sockaddrsList1 = new ArrayList<SocketAddress>();
//        sockaddrsList1.add(new InetSocketAddress("192.168.1.209", 7860));
//        List<SocketAddress> sockaddrsList2 = new ArrayList<SocketAddress>();
//        sockaddrsList2.add(new InetSocketAddress("192.168.1.209", 7870));
//
//        List<EquivalentAddressGroup> addrs = new ArrayList<>();
//        addrs.add(new EquivalentAddressGroup(sockaddrsList1));
//        addrs.add(new EquivalentAddressGroup(sockaddrsList2));

        List<EquivalentAddressGroup> addressGroups = new ArrayList<>();
        HealthClient healthClient = consul.healthClient();
        ConsulResponse<List<ServiceHealth>> listConsulResponse =  healthClient.getHealthyServiceInstances(serviceName);
        Iterator<ServiceHealth> iterator = listConsulResponse.getResponse().iterator();
        while (iterator.hasNext()) {
            Service service1 = iterator.next().getService();
            logger.info("servoce address( " + service1.getAddress()+ ":" + service1.getPort() + " )");
            List<SocketAddress> sockaddrsList = new ArrayList<>();
            sockaddrsList.add(new InetSocketAddress(service1.getAddress(), service1.getPort()));
            addressGroups.add(new EquivalentAddressGroup(sockaddrsList));
            //service1.getAddress();
        }
        this.listener.onAddresses(addressGroups, Attributes.EMPTY);
    }

    private void subscribe(String serviceName) {//
//        serviceHealthCache = ServiceHealthCache
//                .newCache(consul.healthClient(), serviceName);

        serviceHealthCache.addListener((Map<ServiceHealthKey, ServiceHealth> newValues)->{
            serviceList = newValues.values().stream().map(sh ->  sh.getService()).collect(Collectors.toList());
            logger.info(serviceList.toString());
//            serviceList.stream().forEach(service -> {
//
//            });
            List<EquivalentAddressGroup> addressGroups = new ArrayList<>();
            serviceList.forEach(service -> {
                List<SocketAddress> sockaddrsList = new ArrayList<SocketAddress>();
                sockaddrsList.add(new InetSocketAddress(service.getAddress(), service.getPort()));
                addressGroups.add(new EquivalentAddressGroup(sockaddrsList));
            });

            this.listener.onAddresses(addressGroups, Attributes.EMPTY);

            //countDownLatch.countDown();
            // serviceList.clear();
        });
        serviceHealthCache.start();

//        try {
//           // countDownLatch.await();
//            logger.info("Got service");
//        } catch (InterruptedException ex) {
//            logger.warn(ex.getMessage());
//        }
    }

    public static class ConsulNameResolverProvider extends NameResolverProvider {
        private String serviceName;
        private int pauseInSeconds;
        private boolean ignoreConsul;
        private List<String> hostPorts;

        public ConsulNameResolverProvider(String serviceName, List<String> hostPorts) {
            this.serviceName = serviceName;
//            this.pauseInSeconds = pauseInSeconds;
//            this.ignoreConsul = ignoreConsul;
            this.hostPorts = hostPorts;
        }

        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 5;
        }

        @Override
        public String getDefaultScheme() {
            return "consul";
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, Helper helper) {
            //return super.newNameResolver(targetUri, helper);
            return new ConsulNameResolver(targetUri, serviceName);
        }
    }
}
