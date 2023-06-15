package nl.ramsolutions.sw.magik.analysis.typing.io;

/**
 * Package instructions.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
enum InstPackage {

    NAME("name"),
    USES("uses");

    private final String instruction;

    InstPackage(final String instruction) {
        this.instruction = instruction;
    }

    public String getValue() {
        return this.instruction;
    }

}
