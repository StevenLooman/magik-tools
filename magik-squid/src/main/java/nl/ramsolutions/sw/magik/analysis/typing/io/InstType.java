package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Type instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstType {

    TYPE_FORMAT("type_format"),
    TYPE_NAME("type_name"),
    OBJECT("object"),
    INTRINSIC("intrinsic"),
    SLOTTED("slotted"),
    INDEXED("indexed"),
    SLOTS("slots"),
    SLOT_NAME("name"),
    SLOT_TYPE_NAME("type_name"),
    GENERICS("generics"),
    GENERIC_NAME("name"),
    GENERIC_DOC("doc"),
    PARENTS("parents"),
    DOC("doc"),
    MODULE("module");

    private final String value;

    InstType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
