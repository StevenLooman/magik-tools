package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * A key type to be used in a Map.
 */
public class UnaryOperator {

    /**
     * Operator.
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum Operator {

        NOT("~"),
        PLUS("+"),
        MINUS("-"),

        ALLRESULTS_KEYWORD("_ALLRESULTS"),
        NOT_KEYWORD("_not"),
        SCATTER_KEYWORD("_scatter");

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
    private final AbstractType type;
    private final AbstractType resultType;

    /**
     * Constructor.
     * @param operator Operator name.
     * @param type MagikType operator is applied to.
     */
    public UnaryOperator(final Operator operator, final AbstractType type, final AbstractType resultType) {
        this.operator = operator;
        this.resultType = resultType;
        this.type = type;
    }

    /**
     * Get operator.
     * @return Operator.
     */
    public Operator getOperator() {
        return this.operator;
    }

    /**
     * Get type.
     * @return Type.
     */
    public AbstractType getType() {
        return this.type;
    }

    /**
     * Get result type.
     * @return Type.
     */
    public AbstractType getResultType() {
        return this.resultType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.operator, this.type, this.resultType);
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

        final UnaryOperator other = (UnaryOperator) obj;
        return Objects.equals(this.operator, other.operator)
            && Objects.equals(this.type, other.type)
            && Objects.equals(this.resultType, other.resultType);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s, %s)",
            this.getClass().getName(), System.identityHashCode(this),
            this.operator, this.type, this.resultType);
    }

}
