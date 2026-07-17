package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link SlotDefinition}. */
@SuppressWarnings("checkstyle:MagicNumber")
class SlotDefinitionTest {

  private static final Location LOCATION =
      new Location(
          URI.create("memory:///source.magik"), new Range(new Position(1, 1), new Position(1, 2)));
  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");
  private static final TypeString TYPE_STR_B = TypeString.ofIdentifier("b", "user");

  private SlotDefinition createSlotDefinition(
      final Instant timestamp, final String moduleName, final TypeString owner) {
    return new SlotDefinition(
        LOCATION, timestamp, moduleName, null, null, owner, "slot1", TypeString.SW_INTEGER);
  }

  @Test
  void testGetOwnerTypeName() {
    final SlotDefinition slotDef = this.createSlotDefinition(Instant.EPOCH, "module", TYPE_STR_A);
    assertThat(slotDef.getOwnerTypeName()).isEqualTo(TYPE_STR_A);
  }

  @Test
  void testTimestampIgnored() {
    final SlotDefinition slotDef1 = this.createSlotDefinition(Instant.EPOCH, "module", TYPE_STR_A);
    final SlotDefinition slotDef2 =
        this.createSlotDefinition(Instant.parse("2020-01-01T00:00:00Z"), "module", TYPE_STR_A);
    assertThat(slotDef1).isEqualTo(slotDef2);
    assertThat(slotDef1.hashCode()).isEqualTo(slotDef2.hashCode());
  }

  @Test
  void testModuleNameDistinguishes() {
    final SlotDefinition slotDef1 =
        this.createSlotDefinition(Instant.EPOCH, "module_a", TYPE_STR_A);
    final SlotDefinition slotDef2 =
        this.createSlotDefinition(Instant.EPOCH, "module_b", TYPE_STR_A);
    assertThat(slotDef1).isNotEqualTo(slotDef2);
  }

  @Test
  void testOwnerDistinguishes() {
    final SlotDefinition slotDef1 = this.createSlotDefinition(Instant.EPOCH, "module", TYPE_STR_A);
    final SlotDefinition slotDef2 = this.createSlotDefinition(Instant.EPOCH, "module", TYPE_STR_B);
    assertThat(slotDef1).isNotEqualTo(slotDef2);
  }

  @Test
  void testGetBareDefinitionKeepsOwner() {
    final SlotDefinition slotDef = this.createSlotDefinition(Instant.EPOCH, "module", TYPE_STR_A);
    assertThat(slotDef.getBareDefinition().getOwnerTypeName()).isEqualTo(TYPE_STR_A);
  }
}
