package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Default definitions adder.
 */
public final class DefaultDefinitionsAdder {

    private DefaultDefinitionsAdder() {
    }

    /**
     * Add all default definitions.
     * @param definitionKeeper {@link IDefinitionKeeper} to add to.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    public static void addDefaultDefinitions(IDefinitionKeeper definitionKeeper) {
        definitionKeeper.add(new PackageDefinition(null, null, null, null, "sw", Collections.emptyList()));
        definitionKeeper.add(new PackageDefinition(null, null, null, null, "user", List.of("sw")));

        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.OBJECT,
            TypeString.ofIdentifier("object", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("unset", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("false", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("maybe", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("integer", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("bignum", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("float", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.ofIdentifier("symbol", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("character", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("sw_regexp", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("procedure", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.ofIdentifier("char16_vector", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INDEXED,
            TypeString.ofIdentifier("simple_vector", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("heavy_thread", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("light_thread", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("condition", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("enumeration_value", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("indexed_format_mixin", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
        definitionKeeper.add(new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.INTRINSIC,
            TypeString.ofIdentifier("slotted_format_mixin", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));
    }

}
