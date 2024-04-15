package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/** Location within a file. */
public class Location {

  /** Location/Range comparator. */
  public static class LocationRangeComparator implements Comparator<Location>, Serializable {

    @Override
    public int compare(final Location location0, final Location location1) {
      final Location validLocation0 = Location.validLocation(location0);
      final Location validLocation1 = Location.validLocation(location1);

      // Compare URI.
      final URI uri0 = validLocation0.getUri();
      final URI uri1 = validLocation1.getUri();
      if (!uri0.equals(uri1)) {
        return uri0.compareTo(uri1);
      }

      // Compare start position of range.
      final Range range0 = validLocation0.getRange();
      final Range range1 = validLocation1.getRange();
      Objects.requireNonNull(range0);
      Objects.requireNonNull(range1);
      final Position position0 = range0.getStartPosition();
      final Position position1 = range1.getStartPosition();
      return position0.compareTo(position1);
    }
  }

  private final URI uri;
  private final @Nullable Range range;

  /**
   * Constructor.
   *
   * @param uri Path to file.
   * @param range Range in file.
   */
  public Location(final URI uri, final @Nullable Range range) {
    this.uri = uri;
    this.range = range;
  }

  /**
   * Constructor from {@link AstNode}.
   *
   * @param uri Path to file.
   * @param node {@link AstNode} to create {@link Location} from.
   */
  public Location(final URI uri, final AstNode node) {
    this.uri = uri;
    this.range = new Range(node);
  }

  /**
   * Constructor from {@link AstNode}.
   *
   * @param uri Path to file.
   * @param token {@link Token} to create {@link Location} from.
   */
  public Location(final URI uri, final Token token) {
    this.uri = uri;
    this.range = new Range(token);
  }

  /**
   * Constructor, only providing a path.
   *
   * @param uri Path to file.
   */
  public Location(final URI uri) {
    this.uri = uri;
    this.range = null;
  }

  /**
   * Get URI.
   *
   * @return URI to file.
   */
  public URI getUri() {
    return this.uri;
  }

  /**
   * Get Range.
   *
   * @return Range in file.
   */
  @CheckForNull
  public Range getRange() {
    return this.range;
  }

  /**
   * Get path.
   *
   * @return Path to file.
   */
  public Path getPath() {
    return Path.of(this.uri);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getUri(),
        this.getRange());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.uri, this.range);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final Location other = (Location) obj;
    return Objects.equals(this.uri, other.uri) && Objects.equals(this.range, other.range);
  }

  /**
   * Ensure a complete location, with a range.
   *
   * @param location Range to derive from.
   * @return Location.
   */
  public static Location validLocation(final @Nullable Location location) {
    if (location == null) {
      return MagikFile.DEFAULT_LOCATION;
    }

    final Range range = location.getRange();
    if (range == null) {
      return new Location(location.getUri(), new Range(new Position(1, 0), new Position(1, 0)));
    }

    return location;
  }
}
