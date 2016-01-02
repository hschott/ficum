package org.ficum.visitor;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.ficum.node.Node;
import org.ficum.parser.ParseHelper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/petclinic-jpa-model-ctx.xml" })
public class JPATypedQueryVisitorTest {

    @PersistenceContext
    private EntityManager entityManager;

    private JPATypedQueryVisitor<Pet> petVisitor;
    private String[] allowedSelectorNames = { "owner", "type", "address", "telephone", "city", "lastName", "nicknames",
            "firstName", "specialties", "name", "visits", "date", "description", "birthDate", "unknown" };

    @Before
    public void setUp() {
        petVisitor = new JPATypedQueryVisitor<Pet>(Pet.class, entityManager);
    }

    @Test
    public void testAndPredicate() {
        String input = "name=='Chuck',owner.firstName=='Jeff'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testCollectionCount() {
        String input = "visits=ge=2";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test(expected = PersistenceException.class)
    public void testCollectionCountWrongType() {
        String input = "visits=ge=2L";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        petVisitor.start(node);
    }

    @Test
    public void testCollectionLikeString() {
        String input = "nicknames=='*ucky'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testCollectionString() {
        String input = "nicknames=='Chucky'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldDate() {
        String input = "birthDate=gt=2012-08-31";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFieldString() {
        String input = "name=='Max'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldStringLike() {
        String input = "name=='*uck*'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldUnknown() {
        String input = "unknown==1";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        petVisitor.start(node);
    }

    @Test
    public void testNestedEnum() {
        String input = "visits.type=='SCHEDULED'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testNestedTimestamp() {
        DateTime dateTime = new DateTime().withTimeAtStartOfDay().withYear(2013).withMonthOfYear(1).withDayOfMonth(4)
                .withHourOfDay(9).withMinuteOfHour(15).withSecondOfMinute(0);
        String date = ISODateTimeFormat.dateTime().print(dateTime);

        String input = "visits.date==" + date;
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        TypedQuery<Pet> query = petVisitor.start(node);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

}
