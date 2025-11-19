package nl.ramsolutions.sw.loadlist;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;

/** Smallworld load list. */
public class LoadListDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable Instant timestamp;
  private final List<String> filePaths;

  /**
   * Constructor.
   *
   * @param location {@link Location} of the load list.
   * @param timestamp Timestamp of the load list file.
   * @param filePaths List of file paths in the load list.
   */
  public LoadListDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final List<String> filePaths) {
    this.location = location;
    this.timestamp = timestamp;
    this.filePaths = List.copyOf(filePaths);
  }

  public String getName() {
    return this.location != null ? this.location.getUri().toString() : "unknown";
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public IDefinition getBareDefinition() {
    return this;
  }

  public List<String> getFilePaths() {
    return Collections.unmodifiableList(this.filePaths);
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %d files)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getName(),
            this.filePaths.size());
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

    final LoadListDefinition other = (LoadListDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getFilePaths(), this.getFilePaths());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.location, this.filePaths);
  }
}
