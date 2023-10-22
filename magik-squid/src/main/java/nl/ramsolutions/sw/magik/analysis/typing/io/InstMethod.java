package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Method instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstMethod {

    TYPE_NAME("type_name"),
    METHOD_NAME("method_name"),
    DOC("doc"),
    SOURCE_FILE("source_file"),
    MODIFIERS("modifiers"),
    PARAMETERS("parameters"),
    RETURN_TYPES("return_types"),
    LOOP_TYPES("loop_types"),
    MODULE("module");

    private final String value;

    InstMethod(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
