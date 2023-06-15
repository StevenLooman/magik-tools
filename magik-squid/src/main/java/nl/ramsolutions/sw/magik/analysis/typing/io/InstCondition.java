package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Condition instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstCondition {

    NAME("name"),
    DOC("doc"),
    PARENT("parent"),
    SOURCE_FILE("source_file"),
    DATA_NAME_LIST("data_name_list");

    private final String value;

    InstCondition(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
