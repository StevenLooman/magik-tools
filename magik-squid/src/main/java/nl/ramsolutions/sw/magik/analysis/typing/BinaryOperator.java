package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

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
            for (final Operator operator : Operator.values()) {
                if (operator.getValue().equalsIgnoreCase(value)) {
                    return operator;
                }
            }

            for (final Operator operator : Operator.values()) {
                if (operator.name().endsWith("_KEYWORD")) {
                    final String prefixedOperatorValue = "_" + operator.getValue();
                    if (prefixedOperatorValue.equalsIgnoreCase(value)) {
                        return operator;
                    }
                }
            }

            throw new IllegalStateException("Unknown operator: " + value);
        }

    }

    private final Location location;
    private final String moduleName;
    private final Operator operator;
    private final TypeString leftRef;
    private final TypeString rightRef;
    private final TypeString resultRef;
    private final String doc;

    /**
     * Constructor.
     * @param location Location.
     * @param moduleName Module name.
     * @param operator Operator name.
     * @param leftRef Left {@link MagikType}.
     * @param rightRef Right {@link MagikType}.
     */
    public BinaryOperator(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final Operator operator,
            final TypeString leftRef,
            final TypeString rightRef,
            final TypeString resultRef,
            final String doc) {
        this.location = location;
        this.moduleName = moduleName;
        this.operator = operator;
        this.leftRef = leftRef;
        this.rightRef = rightRef;
        this.resultRef = resultRef;
        this.doc = doc;
    }

    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    @CheckForNull
    public String getModuleName() {
        return this.moduleName;
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
    public TypeString getLeftType() {
        return this.leftRef;
    }

    /**
     * Get right type.
     * @return Right type.
     */
    public TypeString getRightType() {
        return this.rightRef;
    }

    /**
     * Get result type.
     * @return Result type.
     */
    public TypeString getResultType() {
        return this.resultRef;
    }

    public String getDoc() {
        return this.doc;
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
            && Objects.equals(this.leftRef, other.leftRef)
            && Objects.equals(this.rightRef, other.rightRef)
            && Objects.equals(this.resultRef, other.resultRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.operator, this.leftRef, this.rightRef, this.resultRef);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s, %s, %s)",
            this.getClass().getName(), System.identityHashCode(this),
            this.operator, this.leftRef, this.rightRef, this.resultRef);
    }

}
