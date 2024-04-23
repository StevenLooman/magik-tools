package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.Location;

/** Base class for definitions. */
public abstract class MagikDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable String moduleName;
  private final @Nullable String doc;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param location Location.
   * @param moduleName Name of the module this definition resides in.
   * @param doc Doc.
   * @param node Node.
   */
  protected MagikDefinition(
      final @Nullable Location location,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node) {
    this.location = location;
    this.moduleName = moduleName;
    this.doc = doc;
    this.node = node;
  }

  /**
   * Get the location of the definition.
   *
   * @return Location of definition.
   */
  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  /**
   * Get the name of the module this definition resides in.
   *
   * @return Module name.
   */
  @CheckForNull
  public String getModuleName() {
    return this.moduleName;
  }

  /**
   * Get doc.
   *
   * @return
   */
  @CheckForNull
  public String getDoc() {
    return this.doc;
  }

  /**
   * Get parsed node.
   *
   * @return
   */
  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  /**
   * Get name of definition.
   *
   * @return Name of definition.
   */
  public abstract String getName();

  /**
   * Get name of package this definition lives in.
   *
   * @return Package name.
   */
  @CheckForNull
  public abstract String getPackage();

  /**
   * Get a(n equal) copy of self, without the {@link AstNode}.
   *
   * @return Copy of self.
   */
  public abstract MagikDefinition getWithoutNode();
}
