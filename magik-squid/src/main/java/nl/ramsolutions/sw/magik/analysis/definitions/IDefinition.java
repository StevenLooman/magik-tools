package nl.ramsolutions.sw.magik.analysis.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.Location;

/** Definition. */
public interface IDefinition {

  @CheckForNull
  Location getLocation();
}
