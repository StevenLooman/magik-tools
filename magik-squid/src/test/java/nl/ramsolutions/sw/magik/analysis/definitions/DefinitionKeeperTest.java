package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link DefinitionKeeper}. */
@SuppressWarnings("checkstyle:MagicNumber")
class DefinitionKeeperTest {

  private static final TypeString TYPE_STR_A = TypeString.ofIdentifier("a", "user");
  private static final TypeString TYPE_STR_B = TypeString.ofIdentifier("b", "user");

  private Location createLocation(final String uri) {
    return new Location(URI.create(uri), new Range(new Position(1, 1), new Position(1, 2)));
  }

  private SlotDefinition createSlotDefinition(
      final String uri, final TypeString owner, final String name, final TypeString type) {
    return new SlotDefinition(
        this.createLocation(uri), Instant.EPOCH, "module", null, null, owner, name, type);
  }

  @Test
  void testAddAndGetSlotDefinitionByOwner() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDef =
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER);
    keeper.add(slotDef);

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).containsExactly(slotDef);
  }

  @Test
  void testGetSlotDefinitionsByUnknownOwnerIsEmpty() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDef =
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER);
    keeper.add(slotDef);

    assertThat(keeper.getSlotDefinitions(TYPE_STR_B)).isEmpty();
  }

  @Test
  void testRemoveSlotDefinition() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDef =
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER);
    keeper.add(slotDef);
    keeper.remove(slotDef);

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).isEmpty();
  }

  @Test
  void testAddSlotDefinitionViaIDefinition() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDef =
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER);
    keeper.add((nl.ramsolutions.sw.IDefinition) slotDef);

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).containsExactly(slotDef);
  }

  @Test
  void testTwoFilesContributeSlotDefinitionsToSameOwner() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDefA =
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER);
    final SlotDefinition slotDefB =
        this.createSlotDefinition("memory:///b.magik", TYPE_STR_A, "slot2", TypeString.SW_FLOAT);
    keeper.add(slotDefA);
    keeper.add(slotDefB);

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).containsExactlyInAnyOrder(slotDefA, slotDefB);
  }

  @Test
  void testAddSlotDefinitionDedupsOnTimestamp() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final Location loc = this.createLocation("memory:///a.magik");
    keeper.add(
        new SlotDefinition(
            loc, Instant.EPOCH, "module", null, null, TYPE_STR_A, "slot1", TypeString.SW_INTEGER));
    keeper.add(
        new SlotDefinition(
            loc,
            Instant.parse("2020-01-01T00:00:00Z"),
            "module",
            null,
            null,
            TYPE_STR_A,
            "slot1",
            TypeString.SW_INTEGER));

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).hasSize(1);
  }

  @Test
  void testClearRemovesSlotDefinitions() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    keeper.add(
        this.createSlotDefinition("memory:///a.magik", TYPE_STR_A, "slot1", TypeString.SW_INTEGER));
    keeper.clear();

    assertThat(keeper.getSlotDefinitions(TYPE_STR_A)).isEmpty();
  }

  @Test
  void testGetDefinitionsByPathFindsSlotDefinition() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final SlotDefinition slotDef =
        new SlotDefinition(
            new Location(
                java.nio.file.Path.of("/tmp/a.magik").toUri(),
                new Range(new Position(1, 1), new Position(1, 2))),
            Instant.EPOCH,
            "module",
            null,
            null,
            TYPE_STR_A,
            "slot1",
            TypeString.SW_INTEGER);
    keeper.add(slotDef);

    assertThat(keeper.getDefinitionsByPath(java.nio.file.Path.of("/tmp/a.magik")))
        .contains(slotDef);
  }
}
