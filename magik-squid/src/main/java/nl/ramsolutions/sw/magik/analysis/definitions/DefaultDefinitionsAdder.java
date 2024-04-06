package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Default definitions adder for {@link IDefinitionKeeper}. */
public final class DefaultDefinitionsAdder {

  private DefaultDefinitionsAdder() {}

  /**
   * Add all default definitions.
   *
   * @param definitionKeeper {@link IDefinitionKeeper} to add to.
   */
  @SuppressWarnings("checkstyle:MethodLength")
  public static void addDefaultDefinitions(IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new PackageDefinition(null, null, null, null, "sw", Collections.emptyList()));
    definitionKeeper.add(new PackageDefinition(null, null, null, null, "user", List.of("sw")));

    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.OBJECT,
            TypeString.SW_OBJECT,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_UNSET,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_FALSE,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_MAYBE,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_INTEGER,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_BIGNUM,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_FLOAT,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_SYMBOL,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_CHARACTER,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_SW_REGEXP,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_PROCEDURE,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_CHAR16_VECTOR,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_SIMPLE_VECTOR,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_HEAVY_THREAD,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_LIGHT_THREAD,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.SW_CONDITION,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.SW_ENUMERATION_VALUE,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_INDEXED_FORMAT_MIXIN,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_SLOTTED_FORMAT_MIXIN,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
  }
}
