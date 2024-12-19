package nl.ramsolutions.sw.magik.analysis.helpers;

public class FlagFlavor {
  public static final String FLAG_READ = ":read";
  public static final String FLAG_READABLE = ":readable";
  public static final String FLAG_WRITE = ":write";
  public static final String FLAG_WRITABLE = ":writable";
  public static final String FLAVOR_PUBLIC = ":public";
  public static final String FLAVOR_PRIVATE = ":private";

  /** :read_only is for slots */
  public static final String FLAVOR_READ_ONLY = ":read_only";

  /** :readonly is for shared variables */
  public static final String FLAVOR_READONLY = ":readonly";

  public static final String TRUE = "_true";
  public static final String FALSE = "_false";

  public static boolean isReadable(String flag) {
    return flag.equals(FLAG_READ) || flag.equals(FLAG_READABLE);
  }

  public static boolean isWritable(String flag) {
    return flag.equals(FLAG_WRITE) || flag.equals(FLAG_WRITABLE);
  }

  public static boolean isPublic(String flavor) {
    return flavor.equals(FLAVOR_PUBLIC) || flavor.equals(FALSE);
  }

  public static boolean isPrivate(String flavor) {
    return flavor.equals(FLAVOR_PRIVATE) || flavor.equals(TRUE);
  }

  /**
   * matches read only for slots
   *
   * @param flavor the flavour
   * @return if it is a :readonly slot
   */
  public static boolean isReadOnly(String flavor) {
    return flavor.equals(FLAVOR_READ_ONLY);
  }

  /**
   * matches read only for shared constants/variables
   *
   * @param flavor the flavour
   * @return if it is a :read_only shared constant/variable
   */
  public static boolean isSharedReadOnly(String flavor) {
    return flavor.equals(FLAVOR_READONLY);
  }
}
