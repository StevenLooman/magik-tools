package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;

/** Smallworld product. */
public class ProductDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable Instant timestamp;
  private final String name;
  private final @Nullable String parent;
  private final @Nullable String version;
  private final @Nullable String versionComment;
  private final @Nullable String title;
  private final @Nullable String description;
  private final List<ProductUsage> usages;

  /**
   * Constructor.
   *
   * @param location Location of definition.
   * @param name Name of product.
   * @param parent Name of parent product.
   * @param version Version.
   * @param versionComment Version comment.
   * @param title Title.
   * @param description Description.
   * @param usages List of used products.
   */
  public ProductDefinition( // NOSONAR
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final String name,
      final @Nullable String parent,
      final @Nullable String version,
      final @Nullable String versionComment,
      final @Nullable String title,
      final @Nullable String description,
      final List<ProductUsage> usages) {
    this.location = location;
    this.timestamp = timestamp;
    this.name = name;
    this.parent = parent;
    this.version = version;
    this.versionComment = versionComment;
    this.title = title;
    this.description = description;
    this.usages = List.copyOf(usages);
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

  @Override
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @CheckForNull
  public String getParent() {
    return parent;
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
   * Get required products for this product.
   *
   * @return Collection of required products for this product.
   */
  public List<ProductUsage> getUsages() {
    return Collections.unmodifiableList(this.usages);
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
        && Objects.equals(otherSwProduct.getUsages(), this.getUsages());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.location, this.name, this.version, this.versionComment);
  }
}
