package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * Binary operator.
 */
public class BinaryOperator {

    /**
     * Operator.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Operator {

        PLUS("+"),
        MINUS("-"),
        STAR("*"),
        DIV("/"),
        EXP("**"),
        EQ("="),
        NEQ("~="),
        NE("<>"),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),

        AND_KEYWORD("and"),
        OR_KEYWORD("or"),
        XOR_KEYWORD("xor"),
        DIV_KEYWORD("div"),
        MOD_KEYWORD("mod"),
        CF_KEYWORD("cf");

        private final String value;

        Operator(final String value) {
            this.value = value.toLowerCase();
        }

        public String getValue() {
            return this.value;
        }

        /**
         * Get Operator for value.
         * @param value Value to get Operator for.
         * @return Operator.
         */
        public static Operator valueFor(final String value) {
            final String valueLower = value.toLowerCase();
            for (final Operator operator : Operator.values()) {
                if (operator.getValue().equals(valueLower)) {
                    return operator;
                }
            }

            throw new IllegalStateException("Unknown operator: " + valueLower);
        }

    }

    private final Operator operator;
    private final AbstractType leftType;
    private final AbstractType rightType;
    private final AbstractType resultType;

    /**
     * Constructor.
     * @param operator Operator name.
     * @param leftType Left {{MagikType}}.
     * @param rightType Right {{MagikType}}.
     */
    public BinaryOperator(
            final Operator operator,
            final AbstractType leftType,
            final AbstractType rightType,
            final AbstractType resultType) {
        this.operator = operator;
        this.leftType = leftType;
        this.rightType = rightType;
        this.resultType = resultType;
    }

    /**
     * Get operator.
     * @return Operator.
     */
    public Operator getOperator() {
        return this.operator;
    }

    /**
     * Get left type.
     * @return Left type.
     */
    public AbstractType getLeftType() {
        return this.leftType;
    }

    /**
     * Get right type.
     * @return Right type.
     */
    public AbstractType getRightType() {
        return this.rightType;
    }

    /**
     * Get result type.
     * @return Result type.
     */
    public AbstractType getResultType() {
        return resultType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final BinaryOperator other = (BinaryOperator) obj;
        return Objects.equals(this.operator, other.operator)
            && Objects.equals(this.leftType, other.leftType)
            && Objects.equals(this.rightType, other.rightType)
            && Objects.equals(this.resultType, other.resultType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.operator, this.leftType, this.rightType, this.resultType);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s, %s, %s)",
            this.getClass().getName(), System.identityHashCode(this),
            this.operator, this.leftType, this.rightType, this.resultType);
    }

}
