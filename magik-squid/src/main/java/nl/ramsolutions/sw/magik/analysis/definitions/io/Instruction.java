package nl.ramsolutions.sw.magik.analysis.definitions.io;

/** Json TypeKeeper Reader/Writer instructions. */
@SuppressWarnings("checkstyle:JavadocVariable")
enum Instruction {
  INSTRUCTION("instruction"),
  PRODUCT("product"),
  MODULE("module"),
  MAGIK_FILE("magik_file"),
  PACKAGE("package"),
  TYPE("type"),
  GLOBAL("global"),
  METHOD("method"),
  PROCEDURE("procedure"),
  CONDITION("condition"),
  BINARY_OPERATOR("binary_operator");

  private final String value;

  Instruction(final String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }

  /**
   * Get enum from value.
   *
   * @param value Value to get enum from.
   * @return Enum.
   */
  public static Instruction fromValue(final String value) {
    for (final Instruction instruction : Instruction.values()) {
      if (instruction.getValue().equals(value)) {
        return instruction;
      }
    }

    throw new IllegalArgumentException("No instruction with value " + value + " found");
  }
}
