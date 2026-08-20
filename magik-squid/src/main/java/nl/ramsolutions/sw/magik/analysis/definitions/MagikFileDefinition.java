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
  private final Provenance provenance;

  public MagikFileDefinition(final Location location, final Instant timestamp) {
    this(location, timestamp, Provenance.UNKNOWN);
  }

  /**
   * Constructor.
   *
   * @param location Location.
   * @param timestamp Timestamp.
   * @param provenance Provenance.
   */
  public MagikFileDefinition(
      final Location location, final Instant timestamp, final Provenance provenance) {
    this.location = location;
    this.timestamp = timestamp;
    this.provenance = provenance;
  }

  @Override
  public Location getLocation() {
    return this.location;
  }

  @Override
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public IDefinition getBareDefinition() {
    return this;
  }

  public URI getUri() {
    return this.location.getUri();
  }

  /**
   * Get the provenance (origin) of this definition.
   *
   * @return Provenance.
   */
  public Provenance getProvenance() {
    return this.provenance;
  }

  /**
   * Return a copy of this definition with the given provenance.
   *
   * @param provenance New provenance.
   * @return Copy with provenance set.
   */
  public MagikFileDefinition withProvenance(final Provenance provenance) {
    return new MagikFileDefinition(this.location, this.timestamp, provenance);
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
