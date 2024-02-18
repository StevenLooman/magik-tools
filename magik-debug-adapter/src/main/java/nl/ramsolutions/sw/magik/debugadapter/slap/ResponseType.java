package nl.ramsolutions.sw.magik.debugadapter.slap;

/** Response type. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ResponseType {
  ERROR(0),
  EVENT(1),
  REPLY(2);

  private final int val;

  ResponseType(final int val) {
    this.val = val;
  }

  public int getVal() {
    return this.val;
  }

  /**
   * Get the {@link ResponseType} from an interger value.
   *
   * @param value Integer value.
   * @return ResponseType
   */
  public static ResponseType valueOf(final int value) {
    for (final ResponseType responseType : ResponseType.values()) {
      if (responseType.getVal() == value) {
        return responseType;
      }
    }

    return null;
  }
}
