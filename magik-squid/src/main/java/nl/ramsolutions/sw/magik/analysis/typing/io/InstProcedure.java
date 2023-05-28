package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Procedure instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstProcedure {

    NAME("name"),
    DOC("doc"),
    PROCEDURE_NAME("procedure_name"),
    SOURCE_FILE("source_file"),
    MODIFIERS("modifiers"),
    PARAMETERS("parameters"),
    RETURN_TYPES("return_types"),
    LOOP_TYPES("loop_types");

    private final String value;

    InstProcedure(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
