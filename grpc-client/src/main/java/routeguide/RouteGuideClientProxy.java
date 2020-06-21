package routeguide;

import com.orbitz.consul.model.health.Service;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RouteGuideClientProxy implements MethodInterceptor {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(RouteGuideClientProxy.class);
    private LoadBalancer loadBalancer;
    private Map<String, ManagedChannel> channels;

    public RouteGuideClientProxy(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
         Optional<Service> service = loadBalancer.getAvailableService();
         service.ifPresent(service1 -> logger.info(service1.toString()));
        Service s  = service.orElseThrow(()-> new RuntimeException("no service"));

        //RouteGuideClient client = (RouteGuideClient)o;
         ((RouteGuideClient) o).setChannel(
                 ManagedChannelBuilder.forAddress(s.getAddress(), s.getPort()).usePlaintext().build()
         );

        return methodProxy.invokeSuper(o, objects);
    }

    public static RouteGuideClient create(LoadBalancer loadBalancer) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(RouteGuideClient.class);
        enhancer.setCallback(new RouteGuideClientProxy(loadBalancer));
        return (RouteGuideClient) enhancer.create();
    }

    public static void main(String[] args) throws InterruptedException {
        Logger.getLogger("io.grpc").setLevel(Level.FINEST);
        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        LoadBalancer loadBalancer = new LoadBalancer("192.168.1.215", 8500);
        loadBalancer.subscribe("RouteGuideServer");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        final int REUESTNUM = 2;

        for (int j = 0; j < 1; j++) {
            executorService.execute(() -> {
                //RouteGuideClient client = new RouteGuideClient("192.168.1.209", 7860, loadBalancer);
                RouteGuideClient client = create(loadBalancer);
                CountDownLatch finishLatch = new CountDownLatch(REUESTNUM);
                long start = System.nanoTime();
                try {
                    for (int i = 0; i < REUESTNUM; i++) {
                        //int lat = getRandomNumberInRange(400146138, 419146138);
                        //int lon = -getRandomNumberInRange(740188906, 749188906);
                        //routeGuideDemo.getFeature(409146138, -746188906);
                        Random random = new Random();
                        int index = random.nextInt(features.size());
                        int lat = features.get(index).getLocation().getLatitude();
                        int lon = features.get(index).getLocation().getLongitude();
                        logger.info("request location: {0} , {1}", lat, lon);
                        //long start = System.nanoTime();
                        client.getFeaturesAsync(lat, lon, finishLatch);
                        //client1.getFeaturesAsync(lat, lon, finishLatch);
                        //long estimatedTime = System.nanoTime() - start;
                        //logger.info("estimatedTime: " + estimatedTime);
                    }
                    finishLatch.await(10, TimeUnit.MINUTES);
                    client.shutdown();

                } catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    //finishLatch.await(10, TimeUnit.MINUTES);
                    long estimatedTime = System.nanoTime() - start;
                    logger.info("estimatedTime: " + estimatedTime);
                }
            });
        }
    }
}
