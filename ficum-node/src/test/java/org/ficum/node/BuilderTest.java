package org.ficum.node;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.junit.Assert;
import org.junit.Test;

public class BuilderTest {

    @Test(expected = IllegalStateException.class)
    public void testAndBeforeConstraint() {
        Builder.newInstance().and();
    }

    @Test()
    public void testAndOperatorConstraint() {
        Node root = Builder.newInstance().constraint("first", Comparison.EQUALS, 1L).and()
                .constraint("second", Comparison.NOT_EQUALS, 2L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(AndNode.class));

        AndNode andNode = (AndNode) root;

        Node left = andNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = andNode.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));
        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;

        Assert.assertEquals("second", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.NOT_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(2l, rightConstraint.getArgument());
    }

    @Test(expected = IllegalStateException.class)
    public void testConstraintAfterConstraint() {
        Builder.newInstance().constraint("first", Comparison.GREATER_EQUALS, 1).constraint("second", Comparison.EQUALS,
                1);
    }

    @Test(expected = IllegalStateException.class)
    public void testEndsubBeforeSub() {
        Builder.newInstance().sub().endsub();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteComparisonConstraint() {
        Builder.newInstance().constraint("first", null, 1L).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteSelectorConstraint() {
        Builder.newInstance().constraint(null, Comparison.EQUALS, 1L).build();
    }

    @Test()
    public void testInfixToPostfix_NaturalOrderLeft() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.OR);
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_NaturalOrderRight() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededNested() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("fifth", Comparison.LESS_THAN, "5"));
        input.push(Operator.AND);
        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("fourth", Comparison.EQUALS, 4f));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.LEFT);
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(Operator.OR);
        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("fifth", Comparison.LESS_THAN, "5"));
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("fourth", Comparison.EQUALS, 4f));
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededOrderLeft() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        input.push(Operator.AND);
        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testInfixToPostfix_PrecededOrderRight() {

        Deque<Object> input = new ArrayDeque<Object>();

        input.push(Operator.RIGHT);
        input.push(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        input.push(Operator.OR);
        input.push(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        input.push(Operator.LEFT);
        input.push(Operator.AND);
        input.push(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Deque<Object> actual = Builder.infixToPostfix(input);

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.add(Operator.AND);
        expected.add(Operator.OR);
        expected.add(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));
        expected.add(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.add(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test()
    public void testIterableComparableConstraint() {
        Builder builder = Builder.newInstance().constraint("first", Comparison.WITHIN, 1, 2.2, 3.3f, "x.x");
        Node root = builder.build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Comparable<?>[] expected = { 1, 2.2, 3.3f, "x.x" };
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testIterableDoubleConstraint() {
        Builder builder = Builder.newInstance().constraint("first", Comparison.WITHIN, 1.1, 2.2, 3.3);
        Node root = builder.build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Double[] expected = { 1.1, 2.2, 3.3 };
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIterableFewConstraint() {
        Double[] array = { 1.1 };
        Builder builder = Builder.newInstance().constraint("first", Comparison.WITHIN, array);
        builder.build();
    }

    @Test()
    public void testIterableIntegerConstraint() {
        Builder builder = Builder.newInstance().constraint("first", Comparison.WITHIN, 1, 2, 3);
        Node root = builder.build();
        Assert.assertTrue(root.getClass().isAssignableFrom(ConstraintNode.class));

        @SuppressWarnings("unchecked")
        ConstraintNode<Iterable<Comparable<?>>> constraintNode = (ConstraintNode<Iterable<Comparable<?>>>) root;
        Integer[] expected = { 1, 2, 3 };
        Assert.assertEquals(Arrays.asList(expected), constraintNode.getArgument());
    }

    @Test()
    public void testNaturalPrecededConstraint() {
        Builder builder = Builder.newInstance().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).and()
                .constraint("third", Comparison.GREATER_EQUALS, 3L);
        Node root = builder.build();
        Assert.assertTrue(root.getClass().isAssignableFrom(OrNode.class));

        OrNode orNode = (OrNode) root;

        Node left = orNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = orNode.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(AndNode.class));
        AndNode andNode = (AndNode) right;
        Assert.assertTrue(andNode.getLeft().getClass().isAssignableFrom(ConstraintNode.class));
        Assert.assertTrue(andNode.getRight().getClass().isAssignableFrom(ConstraintNode.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testOrBeforeConstraint() {
        Builder.newInstance().or();
    }

    @Test()
    public void testOrOperatorConstraint() {
        Node root = Builder.newInstance().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).build();
        Assert.assertTrue(root.getClass().isAssignableFrom(OrNode.class));

        OrNode orNode = (OrNode) root;

        Node left = orNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> leftConstraint = (ConstraintNode<?>) left;
        Assert.assertEquals("first", leftConstraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, leftConstraint.getComparison());
        Assert.assertEquals(1l, leftConstraint.getArgument());

        Node right = orNode.getRight();

        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));
        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;

        Assert.assertEquals("second", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.NOT_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(2l, rightConstraint.getArgument());
    }

    @Test()
    public void testSimpleConstraint() {
        Node n = Builder.newInstance().constraint("first", Comparison.EQUALS, 1L).build();
        Assert.assertTrue(n.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> constraint = (ConstraintNode<?>) n;

        Assert.assertEquals("first", constraint.getSelector());
        Assert.assertEquals(Comparison.EQUALS, constraint.getComparison());
        Assert.assertEquals(1l, constraint.getArgument());
    }

    @Test(expected = IllegalStateException.class)
    public void testSubAfterConstraint() {
        Builder.newInstance().constraint("first", Comparison.GREATER_EQUALS, 1).sub();
    }

    @Test()
    public void testSubPrecededConstraint() {
        Builder builder = Builder.newInstance().sub().constraint("first", Comparison.EQUALS, 1L).or()
                .constraint("second", Comparison.NOT_EQUALS, 2L).endsub().and()
                .constraint("third", Comparison.GREATER_EQUALS, 3L);
        Node root = builder.build();
        Assert.assertTrue(root.getClass().isAssignableFrom(AndNode.class));

        AndNode andNode = (AndNode) root;

        Node right = andNode.getRight();
        Assert.assertTrue(right.getClass().isAssignableFrom(ConstraintNode.class));

        ConstraintNode<?> rightConstraint = (ConstraintNode<?>) right;
        Assert.assertEquals("third", rightConstraint.getSelector());
        Assert.assertEquals(Comparison.GREATER_EQUALS, rightConstraint.getComparison());
        Assert.assertEquals(3l, rightConstraint.getArgument());

        Node left = andNode.getLeft();
        Assert.assertTrue(left.getClass().isAssignableFrom(OrNode.class));

        OrNode orNode = (OrNode) left;
        Assert.assertTrue(orNode.getLeft().getClass().isAssignableFrom(ConstraintNode.class));
        Assert.assertTrue(orNode.getRight().getClass().isAssignableFrom(ConstraintNode.class));
    }

}
