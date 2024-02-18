package nl.ramsolutions.sw.magik.analysis.typing.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.DefinitionKeeperTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import org.junit.jupiter.api.Test;

/** Tests for MagikType. */
class MagikTypeTest {

  @Test
  void testCreateMethodGenericWithRefs() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            "test_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            propertyListRef,
            List.of(),
            List.of()));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            propertyListRef,
            "fast_keys_and_elements()",
            Set.of(),
            List.of(),
            null,
            ExpressionResultString.EMPTY,
            new ExpressionResultString(
                TypeString.ofGenericReference("K"), TypeString.ofGenericReference("E"))));

    final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
    final TypeString propertyListGenericRef =
        TypeString.ofIdentifier(
            "property_list",
            "sw",
            TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
            TypeString.ofGenericDefinition("E", TypeString.SW_FLOAT));
    final AbstractType magikType = typeKeeper.getType(propertyListGenericRef);
    final Method magikMethod = magikType.getMethods("fast_keys_and_elements()").iterator().next();
    final ExpressionResultString loopbodyResultString = magikMethod.getLoopbodyResult();
    // TODO: We should test loopbodyResultString, not TypeReader etc.

    final TypeString kTypeString = loopbodyResultString.get(0, null);
    assertThat(kTypeString).isEqualTo(TypeString.SW_SYMBOL);

    final TypeString eTypeString = loopbodyResultString.get(1, null);
    assertThat(eTypeString).isEqualTo(TypeString.SW_FLOAT);
  }
}
