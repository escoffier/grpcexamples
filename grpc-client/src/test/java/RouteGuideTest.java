
import static org.junit.Assert.assertTrue;

import io.grpc.NameResolverProvider;
import org.junit.Before;
import org.junit.Test;
import routeguide.Feature;
import routeguide.RouteGuideClient;
import routeguide.RouteGuideUtil;
import routeguide.ZooKeeperNameResolver;

import java.io.IOException;
import java.util.List;

public class RouteGuideTest {

    @Before

    @Test
    public void recordRouteTest() {
        List<Feature> allfeatures;
        try {
            allfeatures = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }


        NameResolverProvider nameResolverProvider = new ZooKeeperNameResolver.ZooKeeperNameResolverProvider("RouteGuideService");
        RouteGuideClient client = new RouteGuideClient("127.0.0.1", 2181, nameResolverProvider);
    }
}
