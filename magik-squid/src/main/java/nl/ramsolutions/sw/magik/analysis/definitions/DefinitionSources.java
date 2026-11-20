package nl.ramsolutions.sw.magik.analysis.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.IDefinition;

/**
 * Whether a definition read again from its source is the one the keeper holds, for a re-index.
 *
 * <p>{@code equals} compares a definition's declaration and leaves its usages out, so a body edit
 * that leaves the declaration in place, a method's name included, gives an equal definition with
 * other usages. A re-index keeps the held instance only when its source did not change: equal, and
 * the same usages at the same locations. An inheritance edge compares its origin too.
 */
public final class DefinitionSources {

  /** The usages of one definition, by kind. */
  private record Usages(
      List<MethodUsage> methods,
      List<GlobalUsage> globals,
      List<SlotUsage> slots,
      List<ConditionUsage> conditions,
      List<BinaryOperatorUsage> operators) {}

  private DefinitionSources() {}

  /**
   * Whether {@code parsed} was read from the same source as {@code held}, which it equals.
   *
   * @param held The instance the keeper holds.
   * @param parsed The definition the re-index read.
   * @return True when the held instance may stay.
   */
  public static boolean isSameSource(final IDefinition held, final IDefinition parsed) {
    if (held instanceof final InheritanceDefinition heldEdge
        && parsed instanceof final InheritanceDefinition parsedEdge) {
      return heldEdge.getOrigin() == parsedEdge.getOrigin();
    }

    final Usages heldUsages = DefinitionSources.readUsages(held);
    final Usages parsedUsages = DefinitionSources.readUsages(parsed);
    return Objects.equals(heldUsages, parsedUsages);
  }

  @CheckForNull
  private static Usages readUsages(final IDefinition definition) {
    if (definition instanceof final MethodDefinition method) {
      return new Usages(
          method.getUsedMethods(),
          method.getUsedGlobals(),
          method.getUsedSlots(),
          method.getUsedConditions(),
          method.getUsedBinaryOperators());
    } else if (definition instanceof final ProcedureDefinition procedure) {
      return new Usages(
          procedure.getUsedMethods(),
          procedure.getUsedGlobals(),
          List.of(),
          procedure.getUsedConditions(),
          procedure.getUsedBinaryOperators());
    } else if (definition instanceof final GlobalDefinition global) {
      return new Usages(
          global.getUsedMethods(),
          global.getUsedGlobals(),
          List.of(),
          global.getUsedConditions(),
          global.getUsedBinaryOperators());
    } else if (definition instanceof final TopLevelCodeDefinition topLevelCode) {
      return new Usages(
          topLevelCode.getUsedMethods(),
          topLevelCode.getUsedGlobals(),
          List.of(),
          topLevelCode.getUsedConditions(),
          topLevelCode.getUsedBinaryOperators());
    }

    return null;
  }
}
