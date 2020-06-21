package routeguide;

import io.grpc.NameResolverProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class RouteGuideClientTest {

    List<Feature> allfeatures;

    @Before
    public void setUp() throws Exception {
        try {
            allfeatures = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void recordRoute() {
        List<Feature> features = allfeatures.subList(0,20);
        NameResolverProvider nameResolverProvider = new ZooKeeperNameResolver.ZooKeeperNameResolverProvider("RouteGuideService");
        RouteGuideClient client = new RouteGuideClient("127.0.0.1", 2181, nameResolverProvider);
        try {
            client.recordRoute(allfeatures, 20);
        } catch (InterruptedException ex) {
             ex.printStackTrace();
        }

    }
}