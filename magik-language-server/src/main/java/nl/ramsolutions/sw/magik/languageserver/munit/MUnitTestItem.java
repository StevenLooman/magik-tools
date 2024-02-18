package nl.ramsolutions.sw.magik.languageserver.munit;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.eclipse.lsp4j.Location;

/**
 * Test item supporting a tree structure.
 *
 * <p>Each MUnitTestItem has a unique ID which is unique within its parent. There is no guarantee
 * that test items are unique within the whole hierarchy.
 */
public class MUnitTestItem {

  private final String id;
  private final String label;
  private final Location location;
  private final Set<MUnitTestItem> children = new HashSet<>();

  /**
   * Constructor.
   *
   * @param id ID.
   * @param label Label.
   * @param location Location.
   */
  public MUnitTestItem(final String id, final String label, final @Nullable Location location) {
    this.id = id;
    this.label = label;
    this.location = location;
  }

  public String getId() {
    return this.id;
  }

  public String getLabel() {
    return this.label;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  public Set<MUnitTestItem> getChildren() {
    return Collections.unmodifiableSet(this.children);
  }

  /**
   * Add a child. If a child with the same id already exists, that child is returned and the new
   * child is not added.
   *
   * @param newChild Child to add.
   * @return Existing child if a child with the id exists, or the newly added child.
   */
  public MUnitTestItem addChild(final MUnitTestItem newChild) {
    Optional<MUnitTestItem> optionalChild =
        this.children.stream().filter(child -> child.getId().equals(newChild.getId())).findAny();
    if (optionalChild.isPresent()) {
      return optionalChild.get();
    }

    this.children.add(newChild);
    return newChild;
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

    final MUnitTestItem otherTestItem = (MUnitTestItem) obj;
    return Objects.equals(otherTestItem.getId(), this.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getId());
  }
}
