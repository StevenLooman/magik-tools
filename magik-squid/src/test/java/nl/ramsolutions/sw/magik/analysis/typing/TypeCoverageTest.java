package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeCoverage}. */
class TypeCoverageTest {

  private static final TypeString VALUE = TypeString.ofIdentifier("value", "user");
  private static final TypeString SPECIAL_VALUE = TypeString.ofIdentifier("special_value", "user");
  private static final TypeString OTHER_VALUE = TypeString.ofIdentifier("other_value", "user");
  private static final TypeString UNKNOWN_TYPE = TypeString.ofIdentifier("no_such_type", "user");

  private TypeStringResolver createResolver() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, VALUE);
    this.addExemplar(definitionKeeper, SPECIAL_VALUE);
    this.addExemplar(definitionKeeper, OTHER_VALUE);
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, SPECIAL_VALUE, VALUE));
    return new TypeStringResolver(definitionKeeper);
  }

  private void addExemplar(final IDefinitionKeeper definitionKeeper, final TypeString typeString) {
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeString, null));
  }

  private TypeCoverage.Result getCoverage(
      final TypeCoverage.MemberRule memberRule, final TypeString narrow, final TypeString wide) {
    final TypeStringResolver resolver = this.createResolver();
    final TypeCoverage coverage = new TypeCoverage(resolver, memberRule);
    return coverage.getCoverage(narrow, wide);
  }

  @Test
  void testSubtypeIsCovered() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, SPECIAL_VALUE, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }

  @Test
  void testSameTypeIsCovered() {
    final TypeCoverage.Result result = this.getCoverage(TypeCoverage.MemberRule.ALL, VALUE, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }

  @Test
  void testSupertypeIsNotCovered() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, VALUE, SPECIAL_VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.NOT_COVERED);
  }

  @Test
  void testUnrelatedTypeIsNotCovered() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ANY, OTHER_VALUE, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.NOT_COVERED);
  }

  @Test
  void testAllMembersMustBeCovered() {
    final TypeString narrow = TypeString.ofCombination(SPECIAL_VALUE, OTHER_VALUE);
    final TypeCoverage.Result result = this.getCoverage(TypeCoverage.MemberRule.ALL, narrow, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.NOT_COVERED);
  }

  @Test
  void testAnyMemberSuffices() {
    final TypeString narrow = TypeString.ofCombination(SPECIAL_VALUE, OTHER_VALUE);
    final TypeCoverage.Result result = this.getCoverage(TypeCoverage.MemberRule.ANY, narrow, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }

  @Test
  void testCoveredByOneMemberOfTheWideType() {
    final TypeString wide = TypeString.ofCombination(VALUE, OTHER_VALUE);
    final TypeString narrow = TypeString.ofCombination(SPECIAL_VALUE, OTHER_VALUE);
    final TypeCoverage.Result result = this.getCoverage(TypeCoverage.MemberRule.ALL, narrow, wide);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }

  @Test
  void testUndefinedIsUnknown() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, TypeString.UNDEFINED, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testUndefinedWideTypeIsUnknown() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, VALUE, TypeString.UNDEFINED);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testUnresolvableTypeIsUnknown() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, UNKNOWN_TYPE, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testOneUnknownMemberMakesTheAnswerUnknown() {
    // Under either rule: the unknown member might be the one that decides.
    final TypeString narrow = TypeString.ofCombination(OTHER_VALUE, UNKNOWN_TYPE);
    final TypeCoverage.Result allResult =
        this.getCoverage(TypeCoverage.MemberRule.ALL, narrow, VALUE);
    final TypeCoverage.Result anyResult =
        this.getCoverage(TypeCoverage.MemberRule.ANY, narrow, VALUE);
    assertThat(allResult).isEqualTo(TypeCoverage.Result.UNKNOWN);
    assertThat(anyResult).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testGenericReferenceIsUnknown() {
    final TypeString generic = TypeString.ofGenericReference("E");
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, generic, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testParameterReferenceIsUnknown() {
    final TypeString parameterRef = TypeString.ofParameterRef("a");
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, parameterRef, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testSelfWithoutSelfTypeIsUnknown() {
    final TypeCoverage.Result result =
        this.getCoverage(TypeCoverage.MemberRule.ALL, TypeString.SELF, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.UNKNOWN);
  }

  @Test
  void testSelfStandsForTheSelfType() {
    final TypeStringResolver resolver = this.createResolver();
    final TypeCoverage coverage =
        new TypeCoverage(resolver, TypeCoverage.MemberRule.ALL, SPECIAL_VALUE);
    final TypeCoverage.Result result = coverage.getCoverage(TypeString.SELF, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }

  @Test
  void testGenericsAreIgnored() {
    final TypeString generic = TypeString.ofGenericDefinition("E", VALUE);
    final TypeString narrow = TypeString.ofIdentifier("special_value", "user", generic);
    final TypeCoverage.Result result = this.getCoverage(TypeCoverage.MemberRule.ALL, narrow, VALUE);
    assertThat(result).isEqualTo(TypeCoverage.Result.COVERED);
  }
}
