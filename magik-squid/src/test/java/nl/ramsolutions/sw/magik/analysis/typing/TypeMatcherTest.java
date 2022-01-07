package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IntrinsicType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeMatcher.
 */
class TypeMatcherTest {

    @Test
    void testTypeEquals() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type"));
        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:type"));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotEquals() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type1"));
        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:type2"));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testTypeIsKindOf() {
        final AbstractType baseType = new IntrinsicType(GlobalReference.of("sw:base"));
        final MagikType childType = new IntrinsicType(GlobalReference.of("sw:child"));
        childType.addParent(baseType);

        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:base"));
        final boolean matches = TypeMatcher.typeMatches(childType, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeMatchesCombinedType() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type1"));

        final AbstractType criterium = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type1")),
            new IntrinsicType(GlobalReference.of("sw:type2")));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testTypeNotMatchesCombinedType() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type3"));

        final AbstractType criterium = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type1")),
            new IntrinsicType(GlobalReference.of("sw:type2")));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testCombinedTypeMatchesCombinedType() {
        final AbstractType type = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type1")),
            new IntrinsicType(GlobalReference.of("sw:type2")));

        final AbstractType criterium = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type1")),
            new IntrinsicType(GlobalReference.of("sw:type2")),
            new IntrinsicType(GlobalReference.of("sw:type3")));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isTrue();
    }

    @Test
    void testCombinedTypeNotMatchesCombinedType() {
        final AbstractType type = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type1")),
            new IntrinsicType(GlobalReference.of("sw:type2")));

        final AbstractType criterium = new CombinedType(
            new IntrinsicType(GlobalReference.of("sw:type2")),
            new IntrinsicType(GlobalReference.of("sw:type3")));
        final boolean matches = TypeMatcher.typeMatches(type, criterium);
        assertThat(matches).isFalse();
    }

    @Test
    void testIsKindOfEquals() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type"));
        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:type"));
        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfNotEquals() {
        final AbstractType type = new IntrinsicType(GlobalReference.of("sw:type1"));
        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:type2"));
        final boolean isKindOf = TypeMatcher.isKindOf(type, criterium);
        assertThat(isKindOf).isFalse();
    }

    @Test
    void testIsKindOfDirectParent() {
        final AbstractType baseType = new IntrinsicType(GlobalReference.of("sw:base"));
        final MagikType childType = new IntrinsicType(GlobalReference.of("sw:child"));
        childType.addParent(baseType);

        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:base"));
        final boolean isKindOf = TypeMatcher.isKindOf(childType, criterium);
        assertThat(isKindOf).isTrue();
    }

    @Test
    void testIsKindOfIndirectParent() {
        final AbstractType baseType = new IntrinsicType(GlobalReference.of("sw:base"));
        final MagikType child1Type = new IntrinsicType(GlobalReference.of("sw:child1"));
        child1Type.addParent(baseType);
        final MagikType child2Type = new IntrinsicType(GlobalReference.of("sw:child2"));
        child2Type.addParent(child1Type);

        final AbstractType criterium = new IntrinsicType(GlobalReference.of("sw:base"));
        final boolean isKindOf = TypeMatcher.isKindOf(child2Type, criterium);
        assertThat(isKindOf).isTrue();
    }

}
