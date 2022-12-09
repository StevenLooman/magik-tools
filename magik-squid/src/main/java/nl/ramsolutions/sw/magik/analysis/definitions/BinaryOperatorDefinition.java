package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Binary operator definition.
 */
public class BinaryOperatorDefinition extends Definition {

    private final String operator;
    private final TypeString lhs;
    private final TypeString rhs;

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
            final TypeString lhs,
            final TypeString rhs) {
        super(node, TypeString.UNDEFINED);

        if (!lhs.isSingle()) {
            throw new IllegalStateException();
        }

        if (!rhs.isSingle()) {
            throw new IllegalStateException();
        }

        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public String getOperator() {
        return this.operator;
    }

    public TypeString getLhs() {
        return this.lhs;
    }

    public TypeString getRhs() {
        return this.rhs;
    }

    @Override
    public String getName() {
        return this.lhs.getFullString() + " " + this.operator + " " + this.rhs.getFullString();
    }

}
