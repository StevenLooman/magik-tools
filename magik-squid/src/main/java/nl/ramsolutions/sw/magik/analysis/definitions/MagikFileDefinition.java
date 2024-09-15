package nl.ramsolutions.sw.magik.analysis.definitions;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;

/** Magik file definition. */
public class MagikFileDefinition implements IDefinition {

  private final Location location;
  private final Instant timestamp;

  public MagikFileDefinition(final Location location, final Instant timestamp) {
    this.location = location;
    this.timestamp = timestamp;
  }

  @Override
  public Location getLocation() {
    return this.location;
  }

  @Override
  public Instant getTimestamp() {
    return this.timestamp;
  }

  public URI getUri() {
    return this.location.getUri();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.location, this.timestamp);
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

    final MagikFileDefinition otherMagikFileDef = (MagikFileDefinition) obj;
    return Objects.equals(otherMagikFileDef.getLocation(), this.getLocation())
        && Objects.equals(otherMagikFileDef.getTimestamp(), this.getTimestamp());
  }
}
