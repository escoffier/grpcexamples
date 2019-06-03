package routeguide;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.cache.ServiceHealthCache;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//import java.util.logging.Logger;

public class LoadBalancer {
    private Consul client;
    ServiceHealthCache serviceHealthCache;
    private PositiveAtomicCounter currentIndex;
    //private static Logger logger = Logger.getLogger(LoadBalancer.class.getName());
    private static Logger logger = LoggerFactory.getLogger(LoadBalancer.class);
    private List<Service> serviceList;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    LoadBalancer(String host, int port) {
        client = Consul.builder().withHostAndPort(HostAndPort.fromParts(host,port)).build();
        //HealthClient healthClient = client.healthClient();
        currentIndex = new PositiveAtomicCounter();
    }

//    public HostAndPort getService(String service) {
//        HealthClient healthClient = client.healthClient();
//        ConsulResponse<List<ServiceHealth>> listConsulResponse =  healthClient.getHealthyServiceInstances(service);
//
//        Iterator<ServiceHealth> iterator = listConsulResponse.getResponse().iterator();
//        while (iterator.hasNext()) {
//            Service service1 = iterator.next().getService();
//            logger.info("servoce address( " + service1.getAddress()+ ":" + service1.getPort() + " )");
//            //service1.getAddress();
//        }
//        List<ServiceHealth> healthList = listConsulResponse.getResponse();
//        Service service2;
//        if (!healthList.isEmpty()) {
//            if (currentIndex < healthList.size()) {
//                service2 = healthList.get(currentIndex++).getService();
//            } else {
//                currentIndex = 0;
//                service2 = healthList.get(currentIndex++).getService();
//            }
//            HostAndPort hostAndPort = HostAndPort.fromParts(service2.getAddress(), service2.getPort());
//            return hostAndPort;
//        } else {
//            return HostAndPort.fromHost("1.1.1.1");
//        }
//    }

    public void subscibe(String serviceName) {
        //HealthClient healthClient = client.healthClient();
        serviceHealthCache = ServiceHealthCache
                .newCache(client.healthClient(), serviceName);

        serviceHealthCache.addListener((Map<ServiceHealthKey, ServiceHealth> newValues)->{
            newValues.forEach((ServiceHealthKey key, ServiceHealth serviceHealth) -> {
                List<HealthCheck>  healthChecks = serviceHealth.getChecks();
                serviceHealth.getService();
                healthChecks.forEach((HealthCheck healthCheck) -> {
                    //logger.info("Health notify:  "+ serviceHealth.getService().getService() + " " + serviceHealth.getChecks().ge);
                    logger.info("#######Health Check : " + healthCheck.toString());
                });
                healthChecks.stream().filter((HealthCheck hc) ->{
                    logger.info("######stream-serviceName: " + hc.getServiceName().orElse("no name"));
                    logger.info("#####stream status: " + hc.getStatus());
                   if (hc.getServiceName().get().equals(serviceName) && hc.getStatus().equals("passing")) {
                       logger.info("########(stream)Health Check : " + hc.toString());
                       return true;
                   }
                   return false;
                }).collect(Collectors.toList());
            });
        });
        serviceHealthCache.start();
//        try {
//            countDownLatch.wait();
//        } catch (InterruptedException ex) {
//            logger.warn(ex.getMessage());
//        }
    }

    public void subscribe(String serviceName) {
        serviceHealthCache = ServiceHealthCache
                .newCache(client.healthClient(), serviceName);

        serviceHealthCache.addListener((Map<ServiceHealthKey, ServiceHealth> newValues)->{
            serviceList = newValues.values().stream().map(sh ->  sh.getService()).collect(Collectors.toList());
            logger.info(serviceList.toString());
            countDownLatch.countDown();
            // serviceList.clear();
        });
        serviceHealthCache.start();
        try {
            countDownLatch.await();
            logger.info("Got service");
        } catch (InterruptedException ex) {
            logger.warn(ex.getMessage());
        }
    }

    public Optional<Service> getAvailableService() {
        Service service = null;
        if (!serviceList.isEmpty()) {
            service = serviceList.get(currentIndex.getAndIncrement()%serviceList.size());
//            if (currentIndex < serviceList.size()) {
//                service = serviceList.get(currentIndex);
//                //return Optional.ofNullable(service)
//            }
        }
        return Optional.ofNullable(service);
    }
//    static private class Service{
//        private String name;
//        private String serviceId;
//        private String host;
//        private int port;
//    }
}
