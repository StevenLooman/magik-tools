package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public final class MagikTypedCheckList {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/magiktyped/rules";

  private MagikTypedCheckList() {}

  /**
   * Get the list of {@link MagikCheck}s.
   *
   * @return List of with {@link MagikCheck}s
   */
  public static List<Class<? extends MagikCheck>> getChecks() {
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
   * Get the list of {@link MagikCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public static List<Class<? extends Check>> getBaseChecks() {
    return getChecks().stream()
        .map(clazz -> (Class<? extends Check>) clazz)
        .collect(Collectors.toList());
  }

  /**
   * Get the {@link MagikCheck}s which have a {@link MagikTypedCheckFixer}.
   *
   * @return Map of {@link MagikCheck} and list of {@link MagikTypedCheckFixer}
   */
  public static Map<Class<? extends MagikCheck>, List<Class<? extends MagikTypedCheckFixer>>>
      getFixers() {
    return Map.of(
        TypeDocCheck.class, List.of(TypeDocParameterFixer.class, TypeDocReturnTypeFixer.class));
  }
}
