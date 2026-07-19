package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link MethodDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private MethodDefinition createMethodDefinition(final Instant timestamp) {
    return new MethodDefinition(
        LOCATION,
        timestamp,
        "module",
        null,
        null,
        TYPE_STR_A,
        "method",
        Collections.emptySet(),
        Collections.emptyList(),
        null,
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  @Test
  void testTimestampIgnored() {
    final MethodDefinition methodDef1 = this.createMethodDefinition(Instant.EPOCH);
    final MethodDefinition methodDef2 =
        this.createMethodDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(methodDef1).isEqualTo(methodDef2);
    assertThat(methodDef1.hashCode()).isEqualTo(methodDef2.hashCode());
  }
}
