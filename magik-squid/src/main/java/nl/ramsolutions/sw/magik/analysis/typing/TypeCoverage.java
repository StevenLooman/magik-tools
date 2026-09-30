package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;

/**
 * Tests whether one type is covered by another: whether every value of the narrow type is a kind of
 * the wide type.
 *
 * <p>Unlike {@link TypeStringResolver#isKindOf(TypeString, TypeString)}, which answers {@code
 * false} for a type it cannot resolve, this separates "not covered" from "unknown": {@code
 * _undefined}, an unresolvable type, a generic, parameter or slot reference, a variadic, a tuple or
 * an invokable is unknown, and one unknown member on either side makes the whole answer {@link
 * Result#UNKNOWN}.
 */
public final class TypeCoverage {

  /** Answer of a coverage test. */
  public enum Result {
    /** The narrow type is covered by the wide type. */
    COVERED,
    /** The narrow type is known not to be covered by the wide type. */
    NOT_COVERED,
    /** Either type has a member which cannot be judged. */
    UNKNOWN,
  }

  /** Which members of the narrow type must be covered. */
  public enum MemberRule {
    /** Every member of the narrow type must be covered by some member of the wide type. */
    ALL,
    /** One member of the narrow type covered by some member of the wide type suffices. */
    ANY,
  }

  private final TypeStringResolver resolver;
  private final MemberRule memberRule;
  private final @Nullable TypeString selfType;

  /**
   * Constructor, treating {@code _self} as unknown.
   *
   * @param resolver Resolver to test kinds with.
   * @param memberRule Which members of the narrow type must be covered.
   */
  public TypeCoverage(final TypeStringResolver resolver, final MemberRule memberRule) {
    this(resolver, memberRule, null);
  }

  /**
   * Constructor.
   *
   * @param resolver Resolver to test kinds with.
   * @param memberRule Which members of the narrow type must be covered.
   * @param selfType Type {@code _self} stands for on either side, or {@code null} for unknown.
   */
  public TypeCoverage(
      final TypeStringResolver resolver,
      final MemberRule memberRule,
      final @Nullable TypeString selfType) {
    this.resolver = resolver;
    this.memberRule = memberRule;
    this.selfType = selfType;
  }

  /**
   * Test whether {@code narrow} is covered by {@code wide}.
   *
   * @param narrow Type which should be kind of {@code wide}.
   * @param wide Type which should cover {@code narrow}.
   * @return Coverage of {@code narrow} by {@code wide}.
   */
  public Result getCoverage(final TypeString narrow, final TypeString wide) {
    final List<TypeString> narrowMembers = this.getKnownMembers(narrow);
    final List<TypeString> wideMembers = this.getKnownMembers(wide);
    if (narrowMembers == null || wideMembers == null) {
      return Result.UNKNOWN;
    }

    final Predicate<TypeString> isCovered =
        narrowMember ->
            wideMembers.stream()
                .anyMatch(wideMember -> this.resolver.isKindOf(narrowMember, wideMember));
    final boolean covered =
        this.memberRule == MemberRule.ALL
            ? narrowMembers.stream().allMatch(isCovered)
            : narrowMembers.stream().anyMatch(isCovered);
    return covered ? Result.COVERED : Result.NOT_COVERED;
  }

  /** The members of {@code typeString}, or {@code null} if any of them is unknown. */
  @Nullable
  private List<TypeString> getKnownMembers(final TypeString typeString) {
    final List<TypeString> members = new ArrayList<>();
    for (final TypeString member : typeString.getCombinedTypes()) {
      final TypeString substituted = member.isSelf() ? this.selfType : member;
      if (substituted == null || !this.isKnown(substituted)) {
        return null;
      }

      final TypeString bareMember = substituted.getWithoutGenerics();
      members.add(bareMember);
    }

    return members;
  }

  private boolean isKnown(final TypeString typeString) {
    if (typeString.isUndefined() || TypeCoverage.isPlaceholder(typeString)) {
      return false;
    }

    final Collection<ITypeStringDefinition> definitions = this.resolver.resolve(typeString);
    return !definitions.isEmpty();
  }

  /** A type standing in for another, which only a call site can resolve. */
  private static boolean isPlaceholder(final TypeString typeString) {
    return typeString.isVariadic()
        || typeString.isTuple()
        || typeString.isInvokable()
        || typeString.isGenericReference()
        || typeString.isGenericDefinition()
        || typeString.isParameterReference()
        || typeString.isSlotReference();
  }
}
