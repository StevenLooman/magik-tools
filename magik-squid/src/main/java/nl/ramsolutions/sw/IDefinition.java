package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;

/** Definition. */
public interface IDefinition {

  /**
   * Get the {@link Location} of the definition.
   *
   * @return {@link Location} of the definition
   */
  @CheckForNull
  Location getLocation();

  /**
   * Get the {@link Instant} of the definition.
   *
   * @return {@link Instant} of the definition
   */
  @CheckForNull
  Instant getTimestamp();

  /**
   * Get a(n equal) copy of self, without any additional data such as a AstNode, or self if it does
   * not have any additional data.
   *
   * @return Copy of self.
   */
  IDefinition getBareDefinition();
}
