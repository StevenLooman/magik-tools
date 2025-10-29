package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.magik.CommentedCodeCheck;
import nl.ramsolutions.sw.checks.magik.DuplicateMethodInFileCheck;
import nl.ramsolutions.sw.checks.magik.EmptyBlockCheck;
import nl.ramsolutions.sw.checks.magik.ExemplarSlotCountCheck;
import nl.ramsolutions.sw.checks.magik.FileMethodCountCheck;
import nl.ramsolutions.sw.checks.magik.FileMustStartWithPackageStatementCheck;
import nl.ramsolutions.sw.checks.magik.FileNotInLoadListCheck;
import nl.ramsolutions.sw.checks.magik.ForbiddenCallCheck;
import nl.ramsolutions.sw.checks.magik.ForbiddenGlobalUsageCheck;
import nl.ramsolutions.sw.checks.magik.ForbiddenInheritanceCheck;
import nl.ramsolutions.sw.checks.magik.FormattingCheck;
import nl.ramsolutions.sw.checks.magik.HidesVariableCheck;
import nl.ramsolutions.sw.checks.magik.ImportMissingDefinitionCheck;
import nl.ramsolutions.sw.checks.magik.LhsRhsComparatorEqualCheck;
import nl.ramsolutions.sw.checks.magik.LineLengthCheck;
import nl.ramsolutions.sw.checks.magik.LocalImportProcedureCheck;
import nl.ramsolutions.sw.checks.magik.MethodComplexityCheck;
import nl.ramsolutions.sw.checks.magik.MethodLineCountCheck;
import nl.ramsolutions.sw.checks.magik.MissingPragmaCheck;
import nl.ramsolutions.sw.checks.magik.NestingDepthCheck;
import nl.ramsolutions.sw.checks.magik.NoSelfUseCheck;
import nl.ramsolutions.sw.checks.magik.NoStatementAfterBodyExitCheck;
import nl.ramsolutions.sw.checks.magik.ParameterCountCheck;
import nl.ramsolutions.sw.checks.magik.PragmaDoesNotIncludeMandatoryTopicCheck;
import nl.ramsolutions.sw.checks.magik.PragmaInvalidClassifyLevelCheck;
import nl.ramsolutions.sw.checks.magik.PragmaInvalidUsageCheck;
import nl.ramsolutions.sw.checks.magik.ScopeCountCheck;
import nl.ramsolutions.sw.checks.magik.SimplifyIfCheck;
import nl.ramsolutions.sw.checks.magik.SizeZeroEmptyCheck;
import nl.ramsolutions.sw.checks.magik.SwMethodDocCheck;
import nl.ramsolutions.sw.checks.magik.SyntaxErrorCheck;
import nl.ramsolutions.sw.checks.magik.TodoCommentCheck;
import nl.ramsolutions.sw.checks.magik.TrailingWhitespaceCheck;
import nl.ramsolutions.sw.checks.magik.TypeDocCheck;
import nl.ramsolutions.sw.checks.magik.UndefinedVariableCheck;
import nl.ramsolutions.sw.checks.magik.UnsafeEvaluateInvocationCheck;
import nl.ramsolutions.sw.checks.magik.UnusedVariableCheck;
import nl.ramsolutions.sw.checks.magik.UseValueCompareCheck;
import nl.ramsolutions.sw.checks.magik.VariableCountCheck;
import nl.ramsolutions.sw.checks.magik.VariableDeclarationUsageDistanceCheck;
import nl.ramsolutions.sw.checks.magik.VariableNamingCheck;
import nl.ramsolutions.sw.checks.magik.WarnedCallCheck;
import nl.ramsolutions.sw.checks.magik.fixers.FormattingFixer;
import nl.ramsolutions.sw.checks.magik.fixers.UseValueCompareFixer;

/** Magik {@link Check} list. */
public final class MagikCheckList extends CheckList<MagikCheck, MagikCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "magik";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/magik/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final MagikCheckList INSTANCE = new MagikCheckList();

  private MagikCheckList() {}

  /**
   * Get the list of {@link MagikCheck}s.
   *
   * @return List of {@link MagikCheck}s.
   */
  @Override
  public List<Class<? extends MagikCheck>> getChecks() {
    return List.of(
        CommentedCodeCheck.class,
        DuplicateMethodInFileCheck.class,
        EmptyBlockCheck.class,
        ExemplarSlotCountCheck.class,
        FileMethodCountCheck.class,
        FileMustStartWithPackageStatementCheck.class,
        FileNotInLoadListCheck.class,
        ForbiddenCallCheck.class,
        ForbiddenGlobalUsageCheck.class,
        ForbiddenInheritanceCheck.class,
        FormattingCheck.class,
        MissingPragmaCheck.class,
        HidesVariableCheck.class,
        ImportMissingDefinitionCheck.class,
        LhsRhsComparatorEqualCheck.class,
        LineLengthCheck.class,
        LocalImportProcedureCheck.class,
        MethodComplexityCheck.class,
        MethodLineCountCheck.class,
        NestingDepthCheck.class,
        NoSelfUseCheck.class,
        NoStatementAfterBodyExitCheck.class,
        ParameterCountCheck.class,
        PragmaDoesNotIncludeMandatoryTopicCheck.class,
        PragmaInvalidClassifyLevelCheck.class,
        PragmaInvalidUsageCheck.class,
        ScopeCountCheck.class,
        SimplifyIfCheck.class,
        SizeZeroEmptyCheck.class,
        SwMethodDocCheck.class,
        SyntaxErrorCheck.class,
        TodoCommentCheck.class,
        TrailingWhitespaceCheck.class,
        TypeDocCheck.class,
        UndefinedVariableCheck.class,
        UnsafeEvaluateInvocationCheck.class,
        UnusedVariableCheck.class,
        UseValueCompareCheck.class,
        VariableCountCheck.class,
        VariableDeclarationUsageDistanceCheck.class,
        VariableNamingCheck.class,
        WarnedCallCheck.class);
  }

  /**
   * Get the {@link MagikCheck}s which have a {@link CodeActionSupplier}.
   *
   * @return Map of {@link MagikCheck}s and their {@link CodeActionSupplier}s.
   */
  @Override
  public Map<Class<? extends MagikCheck>, List<Class<? extends MagikCodeActionSupplier>>>
      getFixers() {
    return Map.of(
        FormattingCheck.class, List.of(FormattingFixer.class),
        UseValueCompareCheck.class, List.of(UseValueCompareFixer.class));
  }
}
