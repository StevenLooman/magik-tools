package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.translation.TranslationFileMustStartWithTextEncodingCheck;
import nl.ramsolutions.sw.checks.translation.TranslationIgnoredFieldCheck;
import nl.ramsolutions.sw.checks.translation.TranslationSyntaxErrorCheck;

/** Translation (.trn) file {@link Check} list. */
public class TranslationCheckList
    extends CheckList<TranslationCheck, TranslationCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "sw_translation";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/translation/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TranslationCheckList INSTANCE = new TranslationCheckList();

  private TranslationCheckList() {}

  /**
   * Get the list of {@link TranslationCheck}s.
   *
   * @return List of {@link TranslationCheck}s.
   */
  @Override
  public List<Class<? extends TranslationCheck>> getChecks() {
    return List.of(
        TranslationFileMustStartWithTextEncodingCheck.class,
        TranslationIgnoredFieldCheck.class,
        TranslationSyntaxErrorCheck.class);
  }

  /**
   * Get the {@link TranslationCheck}s which have a {@link TranslationCodeActionSupplier}.
   *
   * @return Map of {@link TranslationCheck}s and their {@link TranslationCodeActionSupplier}s.
   */
  @Override
  public Map<
          Class<? extends TranslationCheck>, List<Class<? extends TranslationCodeActionSupplier>>>
      getFixers() {
    return Map.of();
  }
}
