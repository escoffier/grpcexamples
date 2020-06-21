package ConsulDemo;

import DistributedLocks.DLock;
import DistributedLocks.ZookeeperDLock;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.SessionClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;
import com.orbitz.consul.option.ImmutablePutOptions;
import com.orbitz.consul.option.PutOptions;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LockApp {
    private final static Logger logger = LoggerFactory.getLogger(LockApp.class);
    private Consul consul;

    private static final String     PATH = "/examples/locks";

    //private KVCache kvCache = KVCache.newCache(consul.keyValueClient(), "locker");
    //private SessionClient sessionClient = consul.sessionClient();

    public LockApp(String host, int port)
    {
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port)).build();
    }

    public boolean lock1() {
        Session session = ImmutableSession.builder()
                .name("my-service-lock")
                .ttl("300s")
                .node("foobar")
                .lockDelay("15s").build();

        SessionCreatedResponse response =  consul.sessionClient().createSession(session);

        PutOptions putOptions = ImmutablePutOptions.builder()
                .acquire(response.getId())
                .build();

        return consul.keyValueClient().putValue("lead", "myvalue",0, putOptions);
    }

    public boolean lock() {
        Session session = ImmutableSession.builder()
                .name("my-service-lock")
                .ttl("300s")
                .node("foobar")
                .lockDelay("15s").build();
        SessionCreatedResponse response =  consul.sessionClient().createSession(session);

        return consul.keyValueClient().acquireLock("lead", response.getId());

    }

    public static void testZookeeperLock() {

        final String zookeeperConnectionString = "192.168.254.132:2181,192.168.254.131:2181,192.168.254.131:3181";
        ExecutorService  executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int index = i;
            Callable callable = new Callable() {
                @Override
                public Object call() throws Exception {
                    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
                    CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
                    client.start();
                    DLock dLock = new ZookeeperDLock(client, PATH, "client-" + index);
                    logger.debug("###########  begin to acquire lock");
                    dLock.lock();
                    return null;
                }
            };
            executorService.submit(callable);
        }

        try {
            executorService.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
    }

    public static void testConsulLock() {
        Consul consul = Consul.builder()
                .withHostAndPort(HostAndPort.fromParts("192.168.1.215", 8500))
                .build();
        //DLocker dLocker = new DLocker(consul);
        Thread thread1 = new Thread(()->{
            DLocker locker = new DLocker(consul);
            if (locker.lock("lead")) {
                logger.debug("acquired locker");
                try {
                    Thread.sleep(10*1000*10000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else {
                logger.debug("lock is busy now");
            }
        });

        thread1.start();

        try {
            thread1.join();
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
    }
//    public static void main(String[] args) {
//
//        testZookeeperLock();
//
//    }
}
