package org.ficum.visitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.ficum.node.Node;
import org.ficum.parser.ParseHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.fakemongo.Fongo;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MongoDBVisitorTest.class)
public class MongoDBVisitorTest {

    private MongoClient client;

    private MongoDBVisitor visitor;

    private String[] allowedSelectorNames = { "address", "building", "coord", "street", "zipcode", "borough", "cuisine",
            "grades", "date", "grade", "score", "name", "restaurant_id" };

    private MongoDatabase db;

    @Before
    public void setUp() throws IOException {
        // URLConnection conn = new URL(
        // "https://raw.githubusercontent.com/mongodb/docs-assets/primer-dataset/dataset.json").openConnection();
        // InputStream inputStream = conn.getInputStream();
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("dataset.json");
        List<String> lines = IOUtils.readLines(inputStream);
        inputStream.close();

        client = new Fongo("ficum").getMongo();

        db = client.getDatabase("test");
        MongoCollection<Document> collection = db.getCollection("restaurants");

        List<Document> documents = new ArrayList<Document>();
        for (List<String> slice : Lists.partition(lines, 1000)) {
            for (String json : slice) {
                documents.add(Document.parse(json));
            }
            collection.insertMany(documents, new InsertManyOptions().ordered(false).bypassDocumentValidation(true));
            documents.clear();
        }

        visitor = new MongoDBVisitor();
    }

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testAndPredicate() {
        String input = "name=='Regina Caterers',address.street=='11 Avenue'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(1, db.getCollection("restaurants").count(query));
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='*Kitchen';name=='*Cafe'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(435, db.getCollection("restaurants").count(query));
    }

    @Test
    public void testPrecededOrPredicate() {
        String input = "(name=='*Kitchen';name=='*Cafe'),borough=='Manhattan'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);
        Bson query = visitor.start(node);

        Assert.assertEquals(260, db.getCollection("restaurants").count(query));
    }

}
