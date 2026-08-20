package nl.ramsolutions.sw.magik.analysis.definitions;

/** Origin of a definition. Metadata only — excluded from equals/hashCode. */
public enum Provenance {
  /** Produced by sw_type_dumper (the typing-rich library source). */
  DUMPED,
  /** Read from class_info in product jars (leaner library source). */
  CLASS_INFO,
  /** Indexed from workspace .magik source. */
  INDEXED,
  /** Inferred by the reasoner; never persisted as source. */
  REASONED,
  /** A hand-authored correction that supersedes a library definition. */
  MANUAL,
  /** Origin not recorded (the default for a freshly-constructed, unstamped definition). */
  UNKNOWN
}
