package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Parameter instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstParameter {

    NAME("name"),
    TYPE_NAME("type_name"),
    MODIFIER("modifier");

    private final String value;

    InstParameter(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
