package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;

/**
 * Binary operator definition.
 */
public class BinaryOperatorDefinition extends Definition {

    private final String operator;
    private final String lhs;
    private final String rhs;

    /**
     * Constructor.
     * @param node Node for definition.
     * @param pakkage Package defined in.
     * @param operator Operator name.
     * @param lhs Left Hand Side type.
     * @param rhs Right Hand Side type.
     */
    public BinaryOperatorDefinition(
            final AstNode node,
            final String pakkage,
            final String operator,
            final String lhs,
            final String rhs) {
        super(node, pakkage, lhs + " " + operator + " " + rhs);
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public String getOperator() {
        return this.operator;
    }

    public String getLhs() {
        return this.lhs;
    }

    public String getRhs() {
        return this.rhs;
    }

}
