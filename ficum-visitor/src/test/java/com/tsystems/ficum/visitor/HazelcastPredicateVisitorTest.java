package com.tsystems.ficum.visitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.CacheDeserializedValues;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.tsystems.ficum.node.Node;
import com.tsystems.ficum.parser.ParseHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HazelcastPredicateVisitorTest.class)
public class HazelcastPredicateVisitorTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static HazelcastInstance hazelcastInstance = getHazelcastInstance();

    private HazelcastPredicateVisitor visitor;

    private String[] allowedSelectorNames = { "name", "borough", "address.street", "grade.date", "grade.score" };

    private static HazelcastInstance getHazelcastInstance() {
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.setProperty("hazelcast.phone.home.enabled", "false");

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);

        MapConfig mapConfig = new MapConfig();
        mapConfig.setName("restaurants").setInMemoryFormat(InMemoryFormat.BINARY)
                .setCacheDeserializedValues(CacheDeserializedValues.ALWAYS);
        mapConfig.addMapIndexConfig(new MapIndexConfig("name", false));
        mapConfig.addMapIndexConfig(new MapIndexConfig("borough", false));
        mapConfig.addMapIndexConfig(new MapIndexConfig("address.street", false));
        mapConfig.addMapIndexConfig(new MapIndexConfig("grade.date", true));
        mapConfig.addMapIndexConfig(new MapIndexConfig("grade.score", true));
        config.addMapConfig(mapConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

    protected static IMap<Long, Restaurant> getMap() {
        return hazelcastInstance.getMap("restaurants");
    }

    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        File input = new File(ClassLoader.getSystemResource("db/mongodb/dataset.json").toURI());
        BufferedReader reader = new BufferedReader(new FileReader(input));

        IMap<Long, Restaurant> restaurants = getMap();
        ObjectMapper objectMapper = new ObjectMapper();
        String line;
        while ((line = reader.readLine()) != null) {
            Restaurant restaurant = objectMapper.readValue(line, Restaurant.class);
            restaurants.set(restaurant.getId(), restaurant);
        }

        reader.close();
    }

    @Before
    public void setUp() throws IOException {
        visitor = new HazelcastPredicateVisitor();
    }

    @Test
    public void testAlwaysWildcardPredicate() {
        String input = "name=='Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        visitor.setAlwaysWildcard(true);
        Predicate<?, ?> query = visitor.start(node);
        visitor.setAlwaysWildcard(false);

        Assert.assertEquals(53, getMap().values(query).size());
    }

    @Test
    public void testAndPredicate() {
        String input = "borough=='Manhattan',address.street=='11 Avenue'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(2, getMap().values(query).size());
    }

    @Test
    public void testDatePredicate() {
        String input = "grade.date=ge=2015-01-01,grade.score=gt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(306, getMap().values(query).size());
    }

    @Test
    public void testNotPredicate() {
        String input = "borough!='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(2511, getMap().values(query).size());
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='*Kitchen';name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(249, getMap().values(query).size());
    }

    @Test
    public void testPrecededOrPredicate() {
        String input = "(name=='*Kitchen';name=='*Cafe'),borough=='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(141, getMap().values(query).size());
    }

    @Test
    public void testWildcardPredicate() {
        String input = "name=='*Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate<?, ?> query = visitor.start(node);

        Assert.assertEquals(44, getMap().values(query).size());
    }

}
