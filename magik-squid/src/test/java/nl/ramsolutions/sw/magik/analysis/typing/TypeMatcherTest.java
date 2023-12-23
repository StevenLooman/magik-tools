package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeMatcher.
 */
class TypeMatcherTest {

    @Test
    void testTypeEquals() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString typeRef = TypeString.ofIdentifier("type", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                typeRef,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type = typeKeeper.getType(typeRef);

        final boolean matches = TypeMatcher.typeMatches(type, type);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotEquals() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type = typeKeeper.getType(type1Ref);
        final AbstractType criterium = typeKeeper.getType(type2Ref);

        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testTypeIsKindOf() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final TypeString childRef = TypeString.ofIdentifier("child", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                baseRef,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                childRef,
                List.of(),
                List.of(baseRef),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType baseType = typeKeeper.getType(baseRef);
        final AbstractType childType = typeKeeper.getType(childRef);

        final boolean matches = TypeMatcher.typeMatches(childType, baseType);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeMatchesCombinedType() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type1 = typeKeeper.getType(type1Ref);
        final AbstractType type2 = typeKeeper.getType(type2Ref);

        final AbstractType criterium = new CombinedType(type1, type2);
        final boolean matches = TypeMatcher.typeMatches(type1, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotMatchesCombinedType() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type3Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type1 = typeKeeper.getType(type1Ref);
        final AbstractType type2 = typeKeeper.getType(type2Ref);
        final AbstractType type3 = typeKeeper.getType(type3Ref);

        final AbstractType criterium = new CombinedType(type1, type2);
        final boolean matches = TypeMatcher.typeMatches(type3, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testCombinedTypeMatchesCombinedType() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type3Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type1 = typeKeeper.getType(type1Ref);
        final AbstractType type2 = typeKeeper.getType(type2Ref);
        final AbstractType type3 = typeKeeper.getType(type3Ref);

        final AbstractType type = new CombinedType(type1, type2);
        final AbstractType criterium = new CombinedType(type1, type2, type3);
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testCombinedTypeNotMatchesCombinedType() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type3Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type1 = typeKeeper.getType(type1Ref);
        final AbstractType type2 = typeKeeper.getType(type2Ref);
        final AbstractType type3 = typeKeeper.getType(type3Ref);

        final AbstractType type = new CombinedType(type1, type2);
        final AbstractType criterium = new CombinedType(type2, type3);
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testIsKindOfEquals() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString typeRef = TypeString.ofIdentifier("type", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                typeRef,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type = typeKeeper.getType(typeRef);

        final AbstractType criterium = type;
        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfNotEquals() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type1Ref,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                type2Ref,
                List.of(),
                List.of(),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType type = typeKeeper.getType(type1Ref);
        final AbstractType criterium = typeKeeper.getType(type2Ref);

        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isFalse();
    }

    @Test
    void testIsKindOfDirectParent() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final TypeString childRef = TypeString.ofIdentifier("child", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                baseRef,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                childRef,
                List.of(),
                List.of(baseRef),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType childType = typeKeeper.getType(childRef);
        final AbstractType criterium = typeKeeper.getType(baseRef);

        final boolean isKindOf = TypeMatcher.isKindOf(childType, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfIndirectParent() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final TypeString child1Ref = TypeString.ofIdentifier("child1", "sw");
        final TypeString child2Ref = TypeString.ofIdentifier("child2", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                baseRef,
                List.of(),
                List.of(),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                child1Ref,
                List.of(),
                List.of(baseRef),
                List.of()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.INTRINSIC,
                child2Ref,
                List.of(),
                List.of(child1Ref),
                List.of()));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType baseType = typeKeeper.getType(baseRef);
        final AbstractType child2Type = typeKeeper.getType(child2Ref);

        final AbstractType criterium = baseType;
        final boolean isKindOf = TypeMatcher.isKindOf(child2Type, criterium);
        assertThat(isKindOf).isTrue();
    }

}
