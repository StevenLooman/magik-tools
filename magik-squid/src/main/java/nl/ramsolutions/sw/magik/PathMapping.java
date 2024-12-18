package nl.ramsolutions.sw.magik;

import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathMapping {
  private static final Logger LOGGER = LoggerFactory.getLogger(PathMapping.class);

  private final Path from;
  private final Path to;
  private Boolean writable = false;

  public PathMapping() {
    from = null;
    to = null;
  }

  public PathMapping(String from, String to) {
    this.from = Path.of(from);
    this.to = Path.of(to);
  }

  public PathMapping(String from, String to, Boolean writable) {
    this(from, to);
    this.writable = writable;
  }

  public Path getFrom() {
    return from;
  }

  public Path getTo() {
    return to;
  }

  @SuppressWarnings("unused")
  public Boolean getWritable() {
    return writable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PathMapping that = (PathMapping) o;
    return Objects.equals(getFrom(), that.getFrom()) && Objects.equals(getTo(), that.getTo());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFrom(), getTo());
  }

  public boolean isInvalidPathMapping() {
    return from == null || to == null;
  }

  public Location mapLocation(Location location) {
    String path = location.getPath().toString();
    if (this.isInvalidPathMapping()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Could not use invalid path mapping from: {}, to: {}", from, to);
      }

      return location;
    }

    if (path.startsWith(this.from.toString())) {
      Path newPath = Path.of(path.replace(this.from.toString(), this.to.toString()));
      if (newPath.toFile().exists()) {
        location =
            new Location(
                Path.of(path.replace(this.from.toString(), this.to.toString())).toUri(),
                location.getRange());
      }
    }

    return location;
  }
}
