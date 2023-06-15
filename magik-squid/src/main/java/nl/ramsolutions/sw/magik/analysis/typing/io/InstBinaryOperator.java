package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * BinaryOperator instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstBinaryOperator {

    OPERATOR("operator"),
    LHS_TYPE("lhs_type"),
    RHS_TYPE("rhs_type"),
    RETURN_TYPE("return_type");

    private final String value;

    InstBinaryOperator(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
