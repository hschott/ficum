package org.ficum.node;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * A Builder to help building an {@link Node} tree from an infix stack.
 */
public class Builder {

    private Deque<Object> infixStack;

    private boolean halfopen = true;

    private Builder() {
        infixStack = new ArrayDeque<Object>();
    }

    /**
     * Build a {@link Node} tree from an infix stack
     *
     * @param stack
     * @return {@link Node} root node of the tree
     */
    public static Node build(Iterable<Object> stack) {
        if (stack == null) {
            return null;
        }
        return eval(infixToPostfix(stack));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Node eval(Deque<Object> postfix) {
        while (!postfix.isEmpty()) {
            Object element = postfix.removeFirst();
            if (element instanceof Constraint) {
                Constraint<?> constraint = (Constraint<?>) element;
                return new ConstraintNode(constraint);
            }

            if (element instanceof Operator) {
                Operator operator = (Operator) element;
                OperationNode node = new LogicalOperationNode(operator);
                Node operand1 = eval(postfix);
                Node operand2 = eval(postfix);
                node.setRight(operand1);
                node.setLeft(operand2);
                return node;
            }
        }
        return null;
    }

    protected static Deque<Object> infixToPostfix(Iterable<Object> infix) {
        Deque<Object> output = new ArrayDeque<Object>();
        Deque<Operator> operatorStack = new ArrayDeque<Operator>();

        for (Object element : infix) {
            if (element instanceof Constraint) {
                output.push(element);
            }

            if (element instanceof Operator) {
                Operator op = (Operator) element;

                switch (op) {
                case AND:
                case NOR:
                case LEFT:
                    operatorStack.push(op);
                    break;

                case OR:
                case NAND:
                    while (!operatorStack.isEmpty() && operatorStack.peek().preceded) {
                        output.push(operatorStack.pop());
                    }
                    operatorStack.push(op);
                    break;

                case RIGHT:
                    boolean balanced = false;
                    while (!operatorStack.isEmpty()) {
                        Operator last = operatorStack.pop();
                        if (Operator.LEFT.equals(last)) {
                            balanced = true;
                            break;
                        }
                        output.push(last);
                    }
                    if (!balanced) {
                        throw new IllegalStateException(
                                "Unbalanced subexpression! Make sure subexpressions are closed properly.");
                    }
                    break;

                default:
                    break;
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            output.push(operatorStack.pop());
        }

        return output;
    }

    /**
     * Creates a new instance of the builder.
     *
     * @return {@link Builder} new builder instance
     */
    public static Builder newInstance() {
        return new Builder();
    }

    protected static Deque<Object> reverse(Iterable<Object> stack) {
        Deque<Object> deque = new ArrayDeque<Object>();
        for (Object element : stack) {
            deque.addFirst(element);
        }
        return deque;
    }

    /**
     * Add an AND {@link Operator} to the stack
     *
     * @return {@link Builder} this builder object
     */
    public Builder and() {
        if (halfopen) {
            throw new IllegalStateException("Can not add operator! Expected 'constraint()' or 'sub()'.");
        }
        infixStack.push(Operator.AND);
        halfopen = true;
        return this;
    }

    /**
     * Build a {@link Node} tree
     *
     * @return {@link Node} root node of the tree
     */
    public Node build() {
        if (countOpenSub() > 0) {
            throw new IllegalStateException("Can not build! Close subexpression first.");
        }
        if (halfopen) {
            throw new IllegalStateException("Can not build! Add constraint to complete expression.");
        }
        return eval(infixToPostfix(reverse(infixStack)));
    }

    /**
     * Add a {@link Constraint} to the stack
     *
     * @param selector
     *            identifier for an field this constraint applies to
     * @param comparison
     *            {@link Comparison} to apply
     * @param argument
     *            {@link Comparable} to process against the value of the
     *            selected field
     * @return {@link Builder} this builder object
     */
    public Builder constraint(String selector, Comparison comparison, Comparable<?> argument) {
        if (!halfopen) {
            throw new IllegalStateException("Can not add constraint. Expected 'and()' or 'or()'.");
        }
        infixStack.push(new Constraint<Comparable<?>>(selector, comparison, argument));
        halfopen = false;
        return this;
    }

    /**
     * Add a {@link Constraint} to the stack
     *
     * @param selector
     *            identifier for an field this constraint applies to
     * @param comparison
     *            {@link Comparison} to apply
     * @param argument
     *            Array of Comparable to process against the value of the
     *            selected field
     * @return {@link Builder} this builder object
     */
    public Builder constraint(String selector, Comparison comparison, Comparable<?>... argument) {
        if (argument.length < 2) {
            throw new IllegalArgumentException("Comparable argument array must have at least 2 members.");
        }
        if (!halfopen) {
            throw new IllegalStateException("Can not add constraint. Expected 'and()' or 'or()'.");
        }
        infixStack.push(new Constraint<Iterable<Comparable<?>>>(selector, comparison, Arrays.asList(argument)));
        halfopen = false;
        return this;
    }

    private int countOpenSub() {
        int count = 0;
        for (Object object : infixStack) {
            if (object instanceof Operator) {
                Operator op = (Operator) object;

                switch (op) {
                case LEFT:
                    count++;
                    break;

                case RIGHT:
                    count--;
                    break;

                default:
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Add an RIGHT {@link Operator} to the stack. Think of it as a right
     * parenthesis.
     *
     * @return {@link Builder} this builder object
     */
    public Builder endsub() {
        if (halfopen) {
            throw new IllegalStateException("Can not close subexpression! Expected 'constraint()'.");
        }
        if (countOpenSub() <= 0) {
            throw new IllegalStateException("No open subexpression found!");
        }
        infixStack.push(Operator.RIGHT);
        return this;
    }

    /**
     * Add an OR {@link Operator} to the stack
     *
     * @return {@link Builder} this builder object
     */
    public Builder or() {
        if (halfopen) {
            throw new IllegalStateException("Can not add operator! Expected 'constraint()'.");
        }
        infixStack.push(Operator.OR);
        halfopen = true;
        return this;
    }

    /**
     * Add an LEFT {@link Operator} to the stack. Think of it as a left
     * parenthesis.
     *
     * @return {@link Builder} this builder object
     */
    public Builder sub() {
        if (!halfopen) {
            throw new IllegalStateException("Can not open subexpression! Expected 'and()' or 'or()'.");
        }
        infixStack.push(Operator.LEFT);
        return this;
    }

}
