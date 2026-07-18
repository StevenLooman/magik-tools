package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Default definitions adder for {@link IDefinitionKeeper}. */
public final class DefaultDefinitionsAdder {

  private DefaultDefinitionsAdder() {}

  /**
   * Add base definitions, required for magik-tools itself.
   *
   * @param definitionKeeper {@link IDefinitionKeeper} to add to.
   */
  public static void addBaseDefinitions(final IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new PackageDefinition(
            null, null, null, null, null, TypeString.ANONYMOUS_PACKAGE, Collections.emptyList()));
  }

  /**
   * Add default definitions, required for sanity in magik land.
   *
   * @param definitionKeeper {@link IDefinitionKeeper} to add to.
   */
  @SuppressWarnings("checkstyle:MethodLength")
  public static void addDefaultDefinitions(IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new PackageDefinition(
            null, null, null, null, null, TypeString.SW_PACKAGE, Collections.emptyList()));
    definitionKeeper.add(
        new PackageDefinition(
            null, null, null, null, null, TypeString.USER_PACKAGE, List.of("sw")));

    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.OBJECT,
            TypeString.SW_OBJECT,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_UNSET,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_FALSE,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_MAYBE,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_INTEGER,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_BIGNUM,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_FLOAT,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_SYMBOL,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_CHARACTER,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_SW_REGEXP,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_PROCEDURE,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_CHAR16_VECTOR,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.SW_SIMPLE_VECTOR,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_HEAVY_THREAD,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.SW_LIGHT_THREAD,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.SW_CONDITION,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.SW_ENUMERATION_VALUE,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.MIXIN,
            TypeString.SW_INDEXED_FORMAT_MIXIN,
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.MIXIN,
            TypeString.SW_SLOTTED_FORMAT_MIXIN,
            null));
  }
}
