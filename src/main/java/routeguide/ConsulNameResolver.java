package routeguide;

import com.orbitz.consul.cache.ServiceHealthCache;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ConsulNameResolver extends NameResolver {
    private URI uri;
    private String serviceName;
    private int pauseInSeconds;
    private boolean ignoreConsul;
    private List<String> hostPorts;

    ServiceHealthCache serviceHealthCache;

    public ConsulNameResolver(URI uri) {
        this.uri = uri;
    }

    private Listener listener;

    @Override
    public String getServiceAuthority() {
        return this.uri.getAuthority();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        loadServiceNodes();

    }

    @Override
    public void shutdown() {

    }

    private void loadServiceNodes() {
        List<SocketAddress> sockaddrsList1 = new ArrayList<SocketAddress>();
        sockaddrsList1.add(new InetSocketAddress("192.168.1.209", 7860));
        List<SocketAddress> sockaddrsList2 = new ArrayList<SocketAddress>();
        sockaddrsList2.add(new InetSocketAddress("192.168.1.209", 7870));

        List<EquivalentAddressGroup> addrs = new ArrayList<>();
        addrs.add(new EquivalentAddressGroup(sockaddrsList1));
        addrs.add(new EquivalentAddressGroup(sockaddrsList2));
        this.listener.onAddresses(addrs, Attributes.EMPTY);
    }

    public static class ConsulNameResolverProvider extends NameResolverProvider {
        private String serviceName;
        private int pauseInSeconds;
        private boolean ignoreConsul;
        private List<String> hostPorts;

        public ConsulNameResolverProvider(String serviceName, int pauseInSeconds, boolean ignoreConsul, List<String> hostPorts) {
            this.serviceName = serviceName;
            this.pauseInSeconds = pauseInSeconds;
            this.ignoreConsul = ignoreConsul;
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
            return new ConsulNameResolver(targetUri);
        }
    }
}
