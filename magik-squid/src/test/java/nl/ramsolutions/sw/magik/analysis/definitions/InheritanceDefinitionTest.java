package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link InheritanceDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class InheritanceDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_CHILD = TypeString.ofIdentifier("child", "user");
  private static final TypeString TYPE_STR_PARENT_A = TypeString.ofIdentifier("parent_a", "user");
  private static final TypeString TYPE_STR_PARENT_B = TypeString.ofIdentifier("parent_b", "user");

  private InheritanceDefinition create(
      final Instant timestamp, final String moduleName, final TypeString parent) {
    return new InheritanceDefinition(
        LOCATION, timestamp, moduleName, null, null, TYPE_STR_CHILD, parent);
  }

  @Test
  void testGetters() {
    final InheritanceDefinition def = this.create(Instant.EPOCH, "module", TYPE_STR_PARENT_A);
    assertThat(def.getChildTypeName()).isEqualTo(TYPE_STR_CHILD);
    assertThat(def.getParentTypeName()).isEqualTo(TYPE_STR_PARENT_A);
    assertThat(def.getName()).isEqualTo(TYPE_STR_PARENT_A.getFullString());
  }

  @Test
  void testTimestampIgnored() {
    final InheritanceDefinition def1 = this.create(Instant.EPOCH, "module", TYPE_STR_PARENT_A);
    final InheritanceDefinition def2 =
        this.create(Instant.parse("2020-01-01T00:00:00Z"), "module", TYPE_STR_PARENT_A);
    assertThat(def1).isEqualTo(def2);
    assertThat(def1.hashCode()).isEqualTo(def2.hashCode());
  }

  @Test
  void testModuleNameDistinguishes() {
    final InheritanceDefinition def1 = this.create(Instant.EPOCH, "module_a", TYPE_STR_PARENT_A);
    final InheritanceDefinition def2 = this.create(Instant.EPOCH, "module_b", TYPE_STR_PARENT_A);
    assertThat(def1).isNotEqualTo(def2);
  }

  @Test
  void testParentDistinguishes() {
    final InheritanceDefinition def1 = this.create(Instant.EPOCH, "module", TYPE_STR_PARENT_A);
    final InheritanceDefinition def2 = this.create(Instant.EPOCH, "module", TYPE_STR_PARENT_B);
    assertThat(def1).isNotEqualTo(def2);
  }

  @Test
  void testGetBareDefinitionKeepsTypes() {
    final InheritanceDefinition def = this.create(Instant.EPOCH, "module", TYPE_STR_PARENT_A);
    final InheritanceDefinition bare = def.getBareDefinition();
    assertThat(bare.getChildTypeName()).isEqualTo(TYPE_STR_CHILD);
    assertThat(bare.getParentTypeName()).isEqualTo(TYPE_STR_PARENT_A);
  }
}
