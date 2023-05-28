package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Global instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstGlobal {

    NAME("name"),
    TYPE_NAME("type_name");

    private final String value;

    InstGlobal(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
