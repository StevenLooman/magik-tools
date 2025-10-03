package nl.ramsolutions.sw.typedchecks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.checks.magik.TypeDocCheck;
import nl.ramsolutions.sw.typedchecks.magik.ConditionalExpressionIsFalseTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.DeprecatedMethodUsageTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.DeprecatedTypeUsageTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.GlobalExistsTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.MethodArgumentCountMatchesParameterCountTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.MethodArgumentTypeMatchesParameterTypeTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.MethodExistsTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.MethodIsPublicTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.MethodReturnTypesMatchDocTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.ModuleRequiredForGlobalTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.SlotExistsTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.SwChar16VectorEvaluateInvocationTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.TypeDocTypeExistsTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.UndefinedMethodCallResultTypedCheck;
import nl.ramsolutions.sw.typedchecks.magik.fixers.TypeDocParameterFixer;
import nl.ramsolutions.sw.typedchecks.magik.fixers.TypeDocReturnTypeFixer;

/** Check list. */
public final class MagikTypedCheckList {

  private MagikTypedCheckList() {}

  /**
   * Get the list of {@link MagikCheck}s.
   *
   * @return List of with {@link MagikCheck}s
   */
  public static List<Class<? extends MagikCheck>> getChecks() {
    return List.of(
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
