package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

public class ModuleUsage {

  private final String name;
  private final Location location;

  public ModuleUsage(final String name, final @Nullable Location location) {
    this.name = name;
    this.location = location;
  }

  public String getName() {
    return this.name;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.location);
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

    final ModuleUsage other = (ModuleUsage) obj;
    // Location is not tested!
    return Objects.equals(other.name, this.name);
  }
}
