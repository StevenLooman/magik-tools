package nl.ramsolutions.sw.magik.analysis.typing.types;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;

/** Condition. */
public class Condition {

  private final String moduleName;
  private final Location location;
  private final String name;
  private final List<String> dataNameList;
  private final String parent;
  private final String doc;

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param location Location of condition.
   * @param name Name of condition.
   * @param parent Parent of the condition.
   * @param dataNameList Data names.
   * @param doc Doc.
   */
  public Condition(
      final @Nullable String moduleName,
      final @Nullable Location location,
      final String name,
      final @Nullable String parent,
      final List<String> dataNameList,
      final @Nullable String doc) {
    this.moduleName = moduleName;
    this.location = location;
    this.name = name;
    this.dataNameList = dataNameList;
    this.parent = parent;
    this.doc = doc;
  }

  public String getModuleName() {
    return this.moduleName;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  /**
   * Get name of the condition.
   *
   * @return Name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get data name list.
   *
   * @return Data name list.
   */
  public List<String> getDataNameList() {
    return this.dataNameList;
  }

  /**
   * Get parent.
   *
   * @return Parent.
   */
  @CheckForNull
  public String getParent() {
    return this.parent;
  }

  /**
   * Get doc.
   *
   * @return Doc.
   */
  @CheckForNull
  public String getDoc() {
    return this.doc;
  }
}
