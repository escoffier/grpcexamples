package routeguide;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;

import java.util.Collections;

public class ConsulServiceRegister implements ServiceRegister {

    private final Consul consul;
    private final AgentClient client;
    private String serviceId;
    private Registration service;

    public ConsulServiceRegister(String host, int port, String serverHost, int serverPort, String id) {
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port))
                .build();

        client = consul.agentClient();

        serviceId = "RouteGuideServer-" + id; //+ random.nextInt();
        service = ImmutableRegistration.builder()
                .id(serviceId)
                .name("RouteGuideServer")
                .address(serverHost)
                .port(serverPort)
                .tags(Collections.singletonList("tag1"))
                .meta(Collections.singletonMap("version", "1.0"))
                .check(Registration.RegCheck.grpc(new StringBuilder(serverHost).append(":").append(serverPort).toString(), 10))
                .build();
    }

    public ConsulServiceRegister(RegistryConfig registryConfig) {
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(registryConfig.getHost(), registryConfig.getPort()))
                .build();

        client = consul.agentClient();


    }

//    @Override
//    public void registerService() throws Exception {
//        client.register(service);
//    }

    @Override
    public void registerService(ServiceConfig serviceConfig) throws Exception {
        serviceId = "RouteGuideServer-" + serviceConfig.getServiceId(); //+ random.nextInt();
        service = ImmutableRegistration.builder()
                .id(serviceId)
                .name(serviceConfig.getServiceName())
                .address(serviceConfig.getHost())
                .port(serviceConfig.getPort())
                .tags(Collections.singletonList("tag1"))
                .meta(Collections.singletonMap("version", "1.0"))
                .check(Registration.RegCheck.grpc(new StringBuilder(serviceConfig.getHost())
                        .append(":")
                        .append(serviceConfig.getPort())
                        .toString(), 10))
                .build();
    }

    @Override
    public void unregisterService() {

    }
}
