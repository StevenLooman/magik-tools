package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeMatcher.
 */
class TypeMatcherTest {

    @Test
    void testTypeEquals() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString typeRef = TypeString.ofIdentifier("type", "sw");
        final AbstractType type = new MagikType(typeKeeper, Sort.INTRINSIC, typeRef);

        final boolean matches = TypeMatcher.typeMatches(type, type);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotEquals() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final AbstractType type = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final AbstractType criterium = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);

        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testTypeIsKindOf() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final MagikType baseType = new MagikType(typeKeeper, Sort.INTRINSIC, baseRef);
        final TypeString childRef = TypeString.ofIdentifier("child", "sw");
        final MagikType childType = new MagikType(typeKeeper, Sort.INTRINSIC, childRef);
        childType.addParent(baseRef);

        final boolean matches = TypeMatcher.typeMatches(childType, baseType);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeMatchesCombinedType() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final MagikType type1 = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final MagikType type2 = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);
        final AbstractType criterium = new CombinedType(type1, type2);

        final boolean matches = TypeMatcher.typeMatches(type1, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotMatchesCombinedType() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        final AbstractType type3 = new MagikType(typeKeeper, Sort.INTRINSIC, type3Ref);
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final MagikType type1 = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final MagikType type2 = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);
        final AbstractType criterium = new CombinedType(type1, type2);

        final boolean matches = TypeMatcher.typeMatches(type3, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testCombinedTypeMatchesCombinedType() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final MagikType type1 = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final MagikType type2 = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);
        final AbstractType type = new CombinedType(type1, type2);
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        final MagikType type3 = new MagikType(typeKeeper, Sort.INTRINSIC, type3Ref);
        final AbstractType criterium = new CombinedType(type1, type2, type3);

        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testCombinedTypeNotMatchesCombinedType() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final MagikType type1 = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final MagikType type2 = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);
        final AbstractType type = new CombinedType(type1, type2);
        final TypeString type3Ref = TypeString.ofIdentifier("type3", "sw");
        final MagikType type3 = new MagikType(typeKeeper, Sort.INTRINSIC, type3Ref);
        final AbstractType criterium = new CombinedType(type2, type3);

        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testIsKindOfEquals() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString typeRef = TypeString.ofIdentifier("type", "sw");
        final AbstractType type = new MagikType(typeKeeper, Sort.INTRINSIC, typeRef);
        final AbstractType criterium = type;

        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfNotEquals() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString type1Ref = TypeString.ofIdentifier("type1", "sw");
        final AbstractType type = new MagikType(typeKeeper, Sort.INTRINSIC, type1Ref);
        final TypeString type2Ref = TypeString.ofIdentifier("type2", "sw");
        final AbstractType criterium = new MagikType(typeKeeper, Sort.INTRINSIC, type2Ref);

        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isFalse();
    }

    @Test
    void testIsKindOfDirectParent() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final TypeString childRef = TypeString.ofIdentifier("child", "sw");
        final MagikType childType = new MagikType(typeKeeper, Sort.INTRINSIC, childRef);
        childType.addParent(baseRef);
        final AbstractType criterium = new MagikType(typeKeeper, Sort.INTRINSIC, baseRef);

        final boolean isKindOf = TypeMatcher.isKindOf(childType, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfIndirectParent() {
        final TypeKeeper typeKeeper = new TypeKeeper();
        final TypeString baseRef = TypeString.ofIdentifier("base", "sw");
        final AbstractType baseType = new MagikType(typeKeeper, Sort.INTRINSIC, baseRef);
        final TypeString child1Ref = TypeString.ofIdentifier("child1", "sw");
        final MagikType child1Type = new MagikType(typeKeeper, Sort.INTRINSIC, child1Ref);
        child1Type.addParent(baseRef);
        final TypeString child2Ref = TypeString.ofIdentifier("child2", "sw");
        final MagikType child2Type = new MagikType(typeKeeper, Sort.INTRINSIC, child2Ref);
        child2Type.addParent(child1Ref);
        final AbstractType criterium = baseType;

        final boolean isKindOf = TypeMatcher.isKindOf(child2Type, criterium);
        assertThat(isKindOf).isTrue();
    }

}
