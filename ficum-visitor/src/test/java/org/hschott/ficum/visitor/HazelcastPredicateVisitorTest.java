package org.hschott.ficum.visitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import org.hschott.ficum.node.Node;
import org.hschott.ficum.parser.ParseHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

public class HazelcastPredicateVisitorTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static final HazelcastInstance hazelcastInstance = getHazelcastInstance();

    private HazelcastPredicateVisitor visitor;

    private final String[] allowedSelectorNames = {"name", "borough", "address.street", "address.zipcode", "grade.date", "grade.score"};

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
        mapConfig.addIndexConfig(new IndexConfig(IndexType.HASH, "name"));
        mapConfig.addIndexConfig(new IndexConfig(IndexType.HASH, "borough"));
        mapConfig.addIndexConfig(new IndexConfig(IndexType.HASH, "address.street"));
        mapConfig.addIndexConfig(new IndexConfig(IndexType.HASH, "grade.date"));
        mapConfig.addIndexConfig(new IndexConfig(IndexType.HASH, "grade.score"));
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
    public void setUp() {
        visitor = new HazelcastPredicateVisitor();
    }

    @Test
    public void testAlwaysWildcardPredicate() {
        String input = "name=='Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        visitor.setAlwaysWildcard(true);
        Predicate query = visitor.start(node);
        visitor.setAlwaysWildcard(false);

        Assert.assertEquals(53, getMap().values(query).size());
    }

    @Test
    public void testAndPredicate() {
        String input = "borough=='Manhattan',address.street=='11 Avenue'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(2, getMap().values(query).size());
    }

    @Test
    public void testAndPredicateConcatenation() {
        String input = "borough=='Manhattan',address.street=='11 Avenue',name=='Mcquaids Public House'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(1, getMap().values(query).size());
    }

    @Test
    public void testAndPredicateOrPredicateConcatenation() {
        String input = "borough=='Manhattan',address.street=='11 Avenue';address.street=='East   74 Street',name=='Glorious Food'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(3, getMap().values(query).size());
    }

    @Test
    public void testDatePredicate() {
        String input = "grade.date=ge=2015-01-01,grade.score=gt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(306, getMap().values(query).size());
    }

    @Test
    public void testDateTimePredicate() {
        String input = "grade.date=ge=2015-01-01T00:00:00.000Z,grade.score=gt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(306, getMap().values(query).size());
    }

    @Test
    public void testValueIsNull() {
        String input = "address.zipcode==null";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate query = visitor.start(node);

        Assert.assertEquals(1, getMap().values(query).size());
    }

    @Test
    public void testValueIsNotNull() {
        String input = "address.zipcode!=null";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate query = visitor.start(node);

        Assert.assertEquals(4998, getMap().values(query).size());
    }

    @Test
    public void testNandPredicate() {
        String input = "borough!='Manhattan'.name!='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(2574, getMap().values(query).size());

        input = "borough=='Manhattan';name=='*Cafe'";

        node = ParseHelper.parse(input, allowedSelectorNames);
        query = visitor.start(node);

        Assert.assertEquals(2574, getMap().values(query).size());
    }

    @Test
    public void testNorPredicate() {
        String input = "name=='*Kitchen':name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(4750, getMap().values(query).size());
    }

    @Test
    public void testNotPredicate() {
        String input = "borough!='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(2511, getMap().values(query).size());
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='*Kitchen';name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(249, getMap().values(query).size());
    }

    @Test
    public void testInPredicate() {
        String input = "borough=in=['Queens','Manhattan']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(3475, getMap().values(query).size());
    }

    @Test
    public void testInPredicateSingletonList() {
        String input = "borough=in=['Queens']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(987, getMap().values(query).size());
    }

    @Test
    public void testNinPredicate() {
        String input = "borough=nin=['Queens','Manhattan']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(1524, getMap().values(query).size());
    }

    @Test
    public void testNinPredicateSingletonList() {
        String input = "borough=nin=['Queens']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(4012, getMap().values(query).size());
    }

    @Test
    public void testPrecededOrPredicate() {
        String input = "(name=='*Kitchen';name=='*Cafe'),borough=='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(141, getMap().values(query).size());
    }

    @Test
    public void testWildcardPredicate() {
        String input = "name=='*Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Predicate query = visitor.start(node);

        Assert.assertEquals(44, getMap().values(query).size());
    }

}
