package org.hschott.ficum.visitor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hschott.ficum.node.Node;
import org.hschott.ficum.parser.ParseHelper;
import org.junit.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MongoDBFilterVisitorTest {

    private MongoClient client;

    private MongoDBFilterVisitor visitor;

    private String[] allowedSelectorNames = {"name", "borough", "cuisine", "address.location", "address.street", "address.zipcode",
            "grades.date", "grades.score"};

    private MongoDatabase db;

    protected static MongoCollection<Document> getCollection(MongoDatabase database) {
        return database.getCollection("restaurants");
    }

    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        File input = new File(ClassLoader.getSystemResource("db/mongodb/dataset.json").toURI());
        BufferedReader reader = new BufferedReader(new FileReader(input));

        MongoClient setup = createMongoClient();
        MongoDatabase database = setup.getDatabase("ficum");
        MongoCollection<Document> collection = database.getCollection("restaurants");
        collection.drop();

        String line;
        while ((line = reader.readLine()) != null) {
            collection.insertOne(Document.parse(line));
        }
        reader.close();

        collection.createIndex(Indexes.geo2dsphere("address.location"), new IndexOptions().background(false));
    }

    private static MongoClient createMongoClient() {
        return MongoClients.create("mongodb://127.0.0.1:27017/");
    }

    @Before
    public void setUp() throws IOException {
        client = createMongoClient();
        db = client.getDatabase("ficum");
        visitor = new MongoDBFilterVisitor();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testAlwaysWildcardPredicate() {
        String input = "name=='Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        visitor.setAlwaysWildcard(true);
        Bson query = visitor.start(node);
        visitor.setAlwaysWildcard(false);

        Assert.assertEquals(53, getCollection(db).countDocuments(query));
    }

    @Test
    public void testAndPredicate() {
        String input = "borough=='Manhattan',address.street=='11 Avenue'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).countDocuments(query));
    }

    @Test
    public void testAndPredicateConcatenation() {
        String input = "borough=='Manhattan',address.street=='11 Avenue',name=='Mcquaids Public House'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1, getCollection(db).countDocuments(query));
    }

    @Test
    public void testAndPredicateOrPredicateConcatenation() {
        String input = "borough=='Manhattan',address.street=='11 Avenue';address.street=='East   74 Street',name=='Glorious Food'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(3, getCollection(db).countDocuments(query));
    }

    @Test
    public void testDatePredicate() {
        String input = "grades.date=ge=2015-01-01,grades.score=lt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(11, getCollection(db).countDocuments(query));
    }

    @Test
    public void testDateTimePredicate() {
        String input = "grades.date=ge=2015-01-01T00:00:00.000Z,grades.score=lt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(11, getCollection(db).countDocuments(query));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeometryIntersectFewArgsPredicate() {
        String input = "address.location=ix=[-74.0259567, 40.6353674, -73.9246028]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryIntersectLinePredicate() {
        String input = "address.location=ix=[-74.0259567, 40.6353674, -73.9246028, 40.6522396]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryIntersectOddNumberOfArgsPredicate() {
        String input = "address.location=ix=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781, 40.6522396]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryIntersectPointPredicate() {
        String input = "address.location=ix=[-73.7032601, 40.7386417]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryIntersectPolygonPredicate() {
        String input = "address.location=ix=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryNearMaxMinPredicate() {
        String input = "address.location=nr=[-73.856077, 40.899447, 400.0, 10.0]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);
        
        List results = new ArrayList();
        getCollection(db).find(query).forEach(document -> results.add(document.get("name")));

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testGeometryNearMaxPredicate() {
        String input = "address.location=nr=[-73.856077, 40.848447, 500.0]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        List results = new ArrayList();
        getCollection(db).find(query).forEach(document -> results.add(document.get("name")));

        Assert.assertEquals(4, results.size());
    }

    @Test
    public void testGeometryWithinBoxPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(10, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryWithinCenterPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, " + 500 / (6371.2 * 1000) + "]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4, getCollection(db).countDocuments(query));
    }

    @Test
    public void testGeometryWithinPolygonPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).countDocuments(query));
    }

    @Test
    public void testNandPredicate() {
        String input = "borough!='Manhattan'.name!='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2574, getCollection(db).countDocuments(query));

        input = "borough=='Manhattan';name=='*Cafe'";

        node = ParseHelper.parse(input, allowedSelectorNames);
        query = visitor.start(node);

        Assert.assertEquals(2574, getCollection(db).countDocuments(query));
    }

    @Test
    public void testNorPredicate() {
        String input = "name=='*Kitchen':name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4750, getCollection(db).countDocuments(query));

        input = "name!='*Kitchen',name!='*Cafe'";

        node = ParseHelper.parse(input, allowedSelectorNames);
        query = visitor.start(node);

        Assert.assertEquals(4750, getCollection(db).countDocuments(query));
    }

    @Test
    public void testNotPredicate() {
        String input = "borough!='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2511, getCollection(db).countDocuments(query));
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='*Kitchen';name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(249, getCollection(db).countDocuments(query));
    }

    @Test
    public void testValueIsNull() {
        String input = "address.zipcode==null";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1, getCollection(db).countDocuments(query));
    }

    @Test
    public void testValueIsNotNull() {
        String input = "address.zipcode!=null";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4998, getCollection(db).countDocuments(query));
    }


    @Test
    public void testInPredicate() {
        String input = "borough=in=['Queens','Manhattan']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(3475, getCollection(db).countDocuments(query));
    }

    @Test
    public void testInPredicateSingletonList() {
        String input = "borough=in=['Queens']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(987, getCollection(db).countDocuments(query));
    }

    @Test
    public void testNinPredicate() {
        String input = "borough=nin=['Queens','Manhattan']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1524, getCollection(db).countDocuments(query));
    }

    @Test
    public void testNinPredicateSingletonList() {
        String input = "borough=nin=['Queens']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4012, getCollection(db).countDocuments(query));
    }

    @Test
    public void testPrecededOrPredicate() {
        String input = "(name=='*Kitchen';name=='*Cafe'),borough=='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(141, getCollection(db).countDocuments(query));
    }

    @Test
    public void testWildcardPredicate() {
        String input = "name=='*Kitchen'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(44, getCollection(db).countDocuments(query));
    }
}
