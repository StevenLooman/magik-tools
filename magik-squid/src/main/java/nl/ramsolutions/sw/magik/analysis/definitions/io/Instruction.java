package nl.ramsolutions.sw.magik.analysis.definitions.io;

/** Json TypeKeeper Reader/Writer instructions. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum Instruction {
  PRODUCT(1),
  MODULE(2),
  PACKAGE(3),
  TYPE(4),
  GLOBAL(5),
  METHOD(6),
  PROCEDURE(7),
  CONDITION(8),
  BINARY_OPERATOR(9),
  MAGIK_FILE(10);

  public static final String FIELD_NAME = "i";

  private final Integer value;

  Instruction(final Integer value) {
    this.value = value;
  }

  public Integer getValue() {
    return this.value;
  }

  /**
   * Get enum from value.
   *
   * @param value Value to get enum from.
   * @return Enum.
   */
  public static Instruction fromValue(final Integer value) {
    for (final Instruction instruction : Instruction.values()) {
      if (instruction.getValue().equals(value)) {
        return instruction;
      }
    }

    throw new IllegalArgumentException("No instruction with value " + value + " found");
  }
}
