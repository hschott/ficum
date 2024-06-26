package org.hschott.ficum.visitor;

import org.hschott.ficum.node.AbstractVisitor;
import org.hschott.ficum.node.Node;
import org.hschott.ficum.parser.ParseHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.samples.petclinic.model.Pet;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class JPAPredicateVisitorTest {

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.springframework.samples.petclinic");

    private EntityManager entityManager;

    private CriteriaQuery<Pet> cq;

    private Root<Pet> root;

    private JPAPredicateVisitor<Pet> petVisitor;
    private final String[] allowedSelectorNames = {"nicknames", "owner.firstName", "name", "visits", "visits.type",
            "visits.date", "birthDate", "unknown", "born"};

    private TypedQuery<Pet> getTypedQuery(Predicate predicate) {
        return entityManager.createQuery(
                cq.select(root).distinct(true).where(predicate));
    }

    @Before
    public void setUp() {
        entityManager = emf.createEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        cq = criteriaBuilder.createQuery(Pet.class);
        root = cq.from(Pet.class);
        petVisitor = new JPAPredicateVisitor<>(Pet.class, root, criteriaBuilder);
        petVisitor.addSelectorToFieldMapping("born", "birthDate");
    }

    @Test
    public void testAndPredicate() {
        String input = "name=='Chuck',owner.firstName=='Jeff'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testAndPredicateWithThreeCriteria() {
        String input = "name=='Chuck',owner.firstName=='Jeff',visits.type=='EMERGENCY'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testCollectionCount() {
        String input = "visits=ge=2";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test(expected = PersistenceException.class)
    public void testCollectionCountWrongType() {
        String input = "visits=ge=2L";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        getTypedQuery(predicate);

    }

    @Test
    public void testCollectionLikeString() {
        String input = "nicknames=='*u*'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testCollectionString() {
        String input = "nicknames=='Chucky'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldDate() {
        String input = "birthDate=gt=2012-08-31";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldOffsetDateTime() {
        String input = "birthDate=gt=2012-08-31T00:00:00.000Z";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldString() {
        String input = "name=='Max'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFieldStringAlwaysLike() {
        String input = "name=='uck'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        petVisitor.setAlwaysWildcard(true);
        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        petVisitor.setAlwaysWildcard(false);
        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFieldStringLike() {
        String input = "name=='*uck*'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

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
    public void testValueIsNull() {
        String input = "born==null";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testValueIsNotNull() {
        String input = "born!=null";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(12, results.size());
    }

    @Test
    public void testNandPredicate() {
        String input = "name!='Chuck'.owner.firstName!='Jeff'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testNestedEnum() {
        String input = "visits.type=='SCHEDULED'";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testNestedTimestamp() {
        OffsetDateTime dateTime = OffsetDateTime.of(2013, 1, 4,
                9, 15, 0, 0, ZoneOffset.ofHours(0));
        String date = AbstractVisitor.ISO_OFFSET_DATE_TIME.format(dateTime);

        String input = "visits.date==" + date;
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testNorPredicate() {
        String input = "name!='Leo':owner.firstName=='Jeff'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testOrPredicate() {
        String input = "name=='Leo';owner.firstName=='Jeff'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testAndPredicateCombinedWithOrPredicate() {
        String input = "name=='Chuck';owner.firstName=='Jean',visits.type=='EMERGENCY'";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testInPredicate() {
        String input = "name=in=['Leo','Iggy']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testInPredicateSingletonList() {
        String input = "name=in=['Leo']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testInPredicateWithNestedEnum() {
        String input = "visits.type=in=['SCHEDULED','EMERGENCY']";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(3, results.size());
    }

    @Test
    public void testNinPredicate() {
        String input = "name=nin=['Leo','Iggy']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(11, results.size());
    }

    @Test
    public void testNinPredicateSingletonList() {
        String input = "name=nin=['Leo']";

        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(12, results.size());
    }

    @Test
    public void testSelectorToFieldMapping() {
        String input = "born=gt=2012-08-31";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testLessThan() {
        String input = "born=lt=2012-08-31";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(11, results.size());
    }

    @Test
    public void testGreaterEquals() {
        String input = "born=ge=2010-03-09";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(9, results.size());
    }

    @Test
    public void testLessEquals() {
        String input = "born=le=2010-04-09";
        Node node = ParseHelper.parse(input, allowedSelectorNames);

        Predicate predicate = petVisitor.start(node);
        TypedQuery<Pet> query = getTypedQuery(predicate);

        List<Pet> results = query.getResultList();

        Assert.assertEquals(4, results.size());
    }

}
