package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link BinaryOperatorDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class BinaryOperatorDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");

  private BinaryOperatorDefinition createBinaryOperatorDefinition(final Instant timestamp) {
    return new BinaryOperatorDefinition(
        LOCATION, timestamp, "module", null, null, "+", TYPE_STR_A, TYPE_STR_A, TYPE_STR_A);
  }

  @Test
  void testTimestampIgnored() {
    final BinaryOperatorDefinition binaryOperatorDef1 =
        this.createBinaryOperatorDefinition(Instant.EPOCH);
    final BinaryOperatorDefinition binaryOperatorDef2 =
        this.createBinaryOperatorDefinition(Instant.parse("2020-01-01T00:00:00Z"));
    assertThat(binaryOperatorDef1).isEqualTo(binaryOperatorDef2);
    assertThat(binaryOperatorDef1.hashCode()).isEqualTo(binaryOperatorDef2.hashCode());
  }
}
