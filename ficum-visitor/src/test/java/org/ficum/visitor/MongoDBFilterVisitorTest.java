package org.ficum.visitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.ficum.node.Node;
import org.ficum.parser.ParseHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MongoDBFilterVisitorTest.class)
public class MongoDBFilterVisitorTest {

    private MongoClient client;

    private MongoDBFilterVisitor visitor;

    private String[] allowedSelectorNames = { "address", "building", "location", "street", "zipcode", "borough",
            "cuisine", "grades", "date", "grade", "score", "name", "restaurant_id" };

    private MongoDatabase db;

    protected static MongoCollection<Document> getCollection(MongoDatabase database) {
        return database.getCollection("restaurants");
    }

    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        File input = new File(ClassLoader.getSystemResource("dataset.json").toURI());
        BufferedReader reader = new BufferedReader(new FileReader(input));

        MongoClient setup = new MongoClient(new MongoClientURI("mongodb://127.0.0.1:27017/"));

        MongoDatabase database = setup.getDatabase("ficum");
        MongoCollection<Document> collection = database.getCollection("restaurants");
        if (collection.count() < 4999) {
            database.drop();
            collection = getCollection(database);

            String line;
            while ((line = reader.readLine()) != null) {
                collection.insertOne(Document.parse(line));
            }
            reader.close();

            collection.createIndex(new Document("address.location", "2dsphere"), new IndexOptions().background(false));
        }
        setup.close();
    }

    @Before
    public void setUp() throws IOException {
        client = new MongoClient(new MongoClientURI("mongodb://127.0.0.1:27017"));
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
    public void testAndPredicate() {
        String input = "borough=='Manhattan',address.street=='11 Avenue'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).count(query));
    }

    @Test
    public void testDatePredicate() {
        String input = "grades.date=ge=2015-01-01,grades.score=lt=1";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(11, getCollection(db).count(query));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeometryIntersectFewArgsPredicate() {
        String input = "address.location=ix=[-74.0259567, 40.6353674, -73.9246028]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).count(query));
    }

    @Test
    public void testGeometryIntersectLinePredicate() {
        String input = "address.location=ix=[-74.0259567, 40.6353674, -73.9246028, 40.6522396]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).count(query));
    }

    @Test
    public void testGeometryIntersectOddNumberOfArgsPredicate() {
        String input = "address.location=ix=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781, 40.6522396]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).count(query));
    }

    @Test
    public void testGeometryIntersectPointPredicate() {
        String input = "address.location=ix=[-73.7032601, 40.7386417]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1, getCollection(db).count(query));
    }

    @Test
    public void testGeometryIntersectPolygonPredicate() {
        String input = "address.location=ix=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).count(query));
    }

    @Test
    public void testGeometryNearMaxMinPredicate() {
        String input = "address.location=nr=[-73.856077, 40.899447, 400.0, 10.0]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(2, getCollection(db).count(query));
    }

    @Test
    public void testGeometryNearMaxPredicate() {
        String input = "address.location=nr=[-73.856077, 40.848447, 500.0]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4, getCollection(db).count(query));
    }

    @Test
    public void testGeometryWithinBoxPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(10, getCollection(db).count(query));
    }

    @Test
    public void testGeometryWithinCenterPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, " + 500 / (6371.2 * 1000) + "]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(4, getCollection(db).count(query));
    }

    @Test
    public void testGeometryWithinPolygonPredicate() {
        String input = "address.location=wi=[-73.856077, 40.848447, -73.8786113, 40.8502883, -73.84856870000002, 40.8903781]";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(24, getCollection(db).count(query));
    }

    @Test
    public void testNotPredicate() {
        String input = "name!='*e*'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1149, getCollection(db).count(query));
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='*Kitchen';name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(435, getCollection(db).count(query));
    }

    @Test
    public void testPrecededOrPredicate() {
        String input = "(name=='*Kitchen';name=='*Cafe'),borough=='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(260, getCollection(db).count(query));
    }

}
