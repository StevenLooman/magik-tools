package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.loadlist.LoadListEntryExistsCheck;
import nl.ramsolutions.sw.checks.loadlist.LoadListSyntaxErrorCheck;

/** load_list.txt/patch_list.txt {@link Check} list. */
public class LoadListCheckList extends CheckList<LoadListCheck, LoadListCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "load_list";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/loadlist/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final LoadListCheckList INSTANCE = new LoadListCheckList();

  private LoadListCheckList() {}

  /**
   * Get the list of {@link LoadListCheck}s.
   *
   * @return List of {@link LoadListCheck}s.
   */
  @Override
  public List<Class<? extends LoadListCheck>> getChecks() {
    return List.of(LoadListEntryExistsCheck.class, LoadListSyntaxErrorCheck.class);
  }

  /**
   * Get the {@link LoadListCheck}s which have a {@link LoadListCodeActionSupplier}.
   *
   * @return Map of {@link LoadListCheck}s and their {@link LoadListCodeActionSupplier}s.
   */
  @Override
  public Map<Class<? extends LoadListCheck>, List<Class<? extends LoadListCodeActionSupplier>>>
      getFixers() {
    return Map.of();
  }
}
