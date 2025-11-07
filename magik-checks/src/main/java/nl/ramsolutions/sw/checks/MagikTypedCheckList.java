package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.magik.TypeDocCheck;
import nl.ramsolutions.sw.checks.magiktyped.AssignedTypeDoesNotMatchSlotTypeTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.ComparedTypesDoNotMatchTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.ConditionalExpressionIsFalseTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.DeprecatedMethodUsageTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.DeprecatedTypeUsageTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.GlobalExistsTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.MethodArgumentCountMatchesParameterCountTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.MethodArgumentTypeMatchesParameterTypeTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.MethodExistsTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.MethodIsPublicTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.MethodReturnTypesMatchDocTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.ModuleRequiredForGlobalTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.SlotExistsTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.SwChar16VectorEvaluateInvocationTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.TypeDocTypeExistsTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.UndefinedMethodCallResultTypedCheck;
import nl.ramsolutions.sw.checks.magiktyped.fixers.TypeDocParameterFixer;
import nl.ramsolutions.sw.checks.magiktyped.fixers.TypeDocReturnTypeFixer;

/** Check list. */
public final class MagikTypedCheckList extends CheckList<MagikCheck, MagikTypedCodeActionSupplier> {
  // TODO: Yes, T=MagikCheck. This is due to TypeDocCheck.

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/magiktyped/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final MagikTypedCheckList INSTANCE = new MagikTypedCheckList();

  private MagikTypedCheckList() {}

  /**
   * Get the list of {@link MagikTypedCheck}s.
   *
   * @return List of with {@link MagikTypedCheck}s
   */
  @Override
  public List<Class<? extends MagikCheck>> getChecks() {
    return List.of(
        AssignedTypeDoesNotMatchSlotTypeTypedCheck.class,
        ComparedTypesDoNotMatchTypedCheck.class,
        ConditionalExpressionIsFalseTypedCheck.class,
        DeprecatedMethodUsageTypedCheck.class,
        DeprecatedTypeUsageTypedCheck.class,
        GlobalExistsTypedCheck.class,
        MethodArgumentCountMatchesParameterCountTypedCheck.class,
        MethodArgumentTypeMatchesParameterTypeTypedCheck.class,
        MethodExistsTypedCheck.class,
        MethodIsPublicTypedCheck.class,
        MethodReturnTypesMatchDocTypedCheck.class,
        ModuleRequiredForGlobalTypedCheck.class,
        SlotExistsTypedCheck.class,
        SwChar16VectorEvaluateInvocationTypedCheck.class,
        TypeDocTypeExistsTypedCheck.class,
        UndefinedMethodCallResultTypedCheck.class);
  }

  /**
   * Get the {@link MagikCheck}s which have a {@link MagikTypedCodeActionSupplier}.
   *
   * @return Map of {@link MagikCheck} and list of {@link MagikTypedCodeActionSupplier}
   */
  @Override
  public Map<Class<? extends MagikCheck>, List<Class<? extends MagikTypedCodeActionSupplier>>>
      getFixers() {
    // TODO: For now in MagikTypedCheckList, even it is a regular MagikCheck.
    return Map.of(
        TypeDocCheck.class, List.of(TypeDocParameterFixer.class, TypeDocReturnTypeFixer.class));
  }
}
