package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link GlobalDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class GlobalDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private GlobalDefinition createGlobalDefinition(final Instant timestamp) {
    return new GlobalDefinition(
        LOCATION, timestamp, "module", null, null, TYPE_STR_A, TypeString.SW_INTEGER, null);
  }

  @Test
  void testTimestampIgnored() {
    final GlobalDefinition globalDef1 = this.createGlobalDefinition(Instant.EPOCH);
    final GlobalDefinition globalDef2 =
        this.createGlobalDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(globalDef1).isEqualTo(globalDef2);
    assertThat(globalDef1.hashCode()).isEqualTo(globalDef2.hashCode());
  }
}
