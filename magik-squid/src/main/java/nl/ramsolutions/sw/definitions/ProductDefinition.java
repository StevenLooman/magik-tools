package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;

/** Smallworld product. */
public class ProductDefinition {

  private final String name;
  private final @Nullable Location location;
  private final @Nullable String version;
  private final @Nullable String versionComment;
  private final @Nullable String title;
  private final @Nullable String description;
  private final Set<String> children = new HashSet<>();
  private final Set<String> modules = new HashSet<>();
  private final List<String> requireds;

  /**
   * Constructor.
   *
   * @param location
   * @param name
   * @param version
   * @param versionComment
   */
  public ProductDefinition(
      final @Nullable Location location,
      final String name,
      final @Nullable String version,
      final @Nullable String versionComment,
      final @Nullable String title,
      final @Nullable String description,
      final List<String> requireds) {
    this.name = name;
    this.location = location;
    this.version = version;
    this.versionComment = versionComment;
    this.title = title;
    this.description = description;
    this.requireds = List.copyOf(requireds);
  }

  /**
   * Get name of product.
   *
   * @return Name of product.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get path to {@code product.def} file.
   *
   * @return Path to {@code product.def} file.
   */
  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public String getVersion() {
    return this.version;
  }

  @CheckForNull
  public String getVersionComment() {
    return this.versionComment;
  }

  @CheckForNull
  public String getTitle() {
    return this.title;
  }

  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  /**
   * Get child product definitions of this product.
   *
   * @return Child product definitions of this product.
   */
  public Set<String> getChildren() {
    return Collections.unmodifiableSet(this.children);
  }

  /**
   * Add a {@link ProductDefinition} to this product.
   *
   * @param productName {@link ProductDefinition} to add.
   */
  public void addChild(final String productName) {
    this.children.add(productName);
  }

  /**
   * Get module names in this product.
   *
   * @return Collection of module names in this product.
   */
  public Set<String> getModules() {
    return Collections.unmodifiableSet(this.modules);
  }

  /**
   * Add a module name to this product.
   *
   * @param swModule Modulename to add.
   */
  public void addModule(final String swModule) {
    this.modules.add(swModule);
  }

  /**
   * Get required products for this product.
   *
   * @return Collection of required products for this product.
   */
  public List<String> getRequireds() {
    return Collections.unmodifiableList(this.requireds);
  }

  public void addRequired(final String productName) {
    this.requireds.add(productName);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getName(),
        this.getVersion(),
        this.getVersionComment());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final ProductDefinition otherSwProduct = (ProductDefinition) obj;
    return Objects.equals(otherSwProduct.getLocation(), this.getLocation())
        && Objects.equals(otherSwProduct.getName(), this.getName())
        && Objects.equals(otherSwProduct.getVersion(), this.getVersion())
        && Objects.equals(otherSwProduct.getVersionComment(), this.getVersionComment())
        && Objects.equals(otherSwProduct.getTitle(), this.getTitle())
        && Objects.equals(otherSwProduct.getDescription(), this.getDescription())
        && Objects.equals(otherSwProduct.getRequireds(), this.getRequireds());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.location, this.name, this.version, this.versionComment);
  }
}
