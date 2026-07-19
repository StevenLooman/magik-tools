package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import org.junit.jupiter.api.Test;

/**
 * Test {@link MagikFileDefinition}.
 *
 * <p>Unlike the other definition classes, {@link MagikFileDefinition#equals(Object)} and {@link
 * MagikFileDefinition#hashCode()} both include the timestamp: it identifies which version of the
 * file the definition was read from, so two reads of the same location at different times must not
 * be treated as equal.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class MagikFileDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));

  private MagikFileDefinition createMagikFileDefinition(final Instant timestamp) {
    return new MagikFileDefinition(LOCATION, timestamp);
  }

  @Test
  void testTimestampDistinguishes() {
    final MagikFileDefinition magikFileDef1 = this.createMagikFileDefinition(Instant.EPOCH);
    final MagikFileDefinition magikFileDef2 =
        this.createMagikFileDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(magikFileDef1).isNotEqualTo(magikFileDef2);
  }
}
